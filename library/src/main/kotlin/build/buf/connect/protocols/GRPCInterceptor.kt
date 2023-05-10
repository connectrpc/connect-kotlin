// Copyright 2022-2023 Buf Technologies, Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package build.buf.connect.protocols

import build.buf.connect.Code
import build.buf.connect.ConnectError
import build.buf.connect.Headers
import build.buf.connect.Interceptor
import build.buf.connect.ProtocolClientConfig
import build.buf.connect.StreamFunction
import build.buf.connect.StreamResult
import build.buf.connect.Trailers
import build.buf.connect.UnaryFunction
import build.buf.connect.compression.CompressionPool
import build.buf.connect.http.HTTPResponse
import okio.Buffer

/**
 * The gRPC HTTP implementation
 * https://github.com/grpc/grpc/blob/master/doc/PROTOCOL-HTTP2.md
 */
internal class GRPCInterceptor(
    private val clientConfig: ProtocolClientConfig
) : Interceptor {
    private val serializationStrategy = clientConfig.serializationStrategy
    private val completionParser = GRPCCompletionParser(serializationStrategy.errorDetailParser())
    private var responseCompressionPool: CompressionPool? = null

    override fun unaryFunction(): UnaryFunction {
        return UnaryFunction(
            requestFunction = { request ->
                val requestHeaders = mutableMapOf<String, List<String>>()
                requestHeaders.putAll(request.headers)
                if (clientConfig.compressionPools().isNotEmpty()) {
                    requestHeaders.put(
                        GRPC_ACCEPT_ENCODING,
                        clientConfig.compressionPools()
                            .map { compressionPool -> compressionPool.name() }
                    )
                }
                val requestMessage = Buffer().use { buffer ->
                    if (request.message != null) {
                        buffer.write(request.message)
                    }
                    buffer
                }
                val requestCompression = clientConfig.requestCompression
                // GRPC unary payloads are enveloped.
                val envelopedMessage = Envelope.pack(
                    requestMessage,
                    requestCompression?.compressionPool,
                    requestCompression?.minBytes
                )
                request.clone(
                    url = request.url,
                    // The underlying content type is overridden here.
                    contentType = "application/grpc+${serializationStrategy.serializationName()}",
                    headers = requestHeaders.withGRPCRequestHeaders(),
                    message = envelopedMessage.readByteArray()
                )
            },
            responseFunction = { response ->
                if (response.code != Code.OK) {
                    return@UnaryFunction response
                }
                val trailers = response.trailers
                val completion = completionParser.parse(trailers)
                val code = completion?.code ?: Code.UNKNOWN
                val responseHeaders = response.headers.toMutableMap()
                val compressionPool =
                    clientConfig.compressionPool(responseHeaders[GRPC_ENCODING]?.first())
                if (code == Code.OK) {
                    val (_, message) = Envelope.unpackWithHeaderByte(
                        response.message.buffer,
                        compressionPool
                    )
                    HTTPResponse(
                        code = code,
                        headers = responseHeaders,
                        message = message,
                        trailers = trailers,
                        error = response.error,
                        tracingInfo = response.tracingInfo
                    )
                } else {
                    val result = Buffer()
                    if (completion != null) {
                        val errorMessage = completion.message
                        result.write(errorMessage)
                    }
                    HTTPResponse(
                        code = code,
                        headers = responseHeaders,
                        message = result,
                        trailers = trailers,
                        error = ConnectError(
                            code = code,
                            errorDetailParser = serializationStrategy.errorDetailParser(),
                            message = completion?.message?.utf8(),
                            details = completion?.errorDetails ?: emptyList()
                        ),
                        tracingInfo = response.tracingInfo
                    )
                }
            }
        )
    }

    override fun streamFunction(): StreamFunction {
        return StreamFunction(
            requestFunction = { request ->
                request.clone(
                    url = request.url,
                    contentType = "application/grpc+${serializationStrategy.serializationName()}",
                    headers = request.headers.withGRPCRequestHeaders(),
                    message = request.message
                )
            },
            requestBodyFunction = { buffer ->
                val requestCompression = clientConfig.requestCompression
                Envelope.pack(buffer, requestCompression?.compressionPool, requestCompression?.minBytes)
            },
            streamResultFunction = { res ->
                val streamResult = res.fold(
                    onHeaders = { result ->
                        val responseHeaders = result.headers.filter { entry -> !entry.key.startsWith("trailer") }.toMutableMap()
                        responseCompressionPool = clientConfig.compressionPool(responseHeaders[GRPC_ENCODING]?.first())
                        StreamResult.Headers(responseHeaders)
                    },
                    onMessage = { result ->
                        val (_, unpackedMessage) = Envelope.unpackWithHeaderByte(
                            result.message,
                            responseCompressionPool
                        )
                        StreamResult.Message(unpackedMessage)
                    },
                    onCompletion = { result ->
                        val streamTrailers: Trailers = result.trailers
                        val completion = completionParser.parse(streamTrailers)
                        val code = completion?.code ?: Code.UNKNOWN
                        val message = completion?.message
                        val details = completion?.errorDetails
                        val connectError = if (result.connectError() != null) {
                            result.connectError()
                        } else if (result.error != null || code != Code.OK) {
                            ConnectError(
                                code = code,
                                errorDetailParser = serializationStrategy.errorDetailParser(),
                                message = message?.utf8(),
                                exception = result.error,
                                details = details ?: emptyList(),
                                metadata = streamTrailers
                            )
                        } else {
                            // Successful call.
                            null
                        }
                        StreamResult.Complete(
                            code = connectError?.code ?: Code.OK,
                            error = connectError,
                            trailers = streamTrailers
                        )
                    }
                )
                streamResult
            }
        )
    }

    private fun Headers.withGRPCRequestHeaders(): Headers {
        val headers = toMutableMap()
        // Note that we do not comply with the recommended structure for user-agent:
        // https://github.com/grpc/grpc/blob/v1.51.1/doc/PROTOCOL-HTTP2.md#user-agents
        // But this behavior matches connect-web:
        // https://github.com/bufbuild/connect-web/blob/v0.4.0/packages/connect-core/src/grpc-web-create-request-header.ts#L33-L36
        headers[GRPC_USER_AGENT] = listOf("@bufbuild/connect-kotlin")
        headers[GRPC_TE_HEADER] = listOf("trailers")
        val requestCompression = clientConfig.requestCompression
        if (requestCompression != null) {
            headers[GRPC_ENCODING] = listOf(requestCompression.compressionPool.name())
        }
        return headers
    }
}

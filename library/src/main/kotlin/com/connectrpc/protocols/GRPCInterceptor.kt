// Copyright 2022-2023 The Connect Authors
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

package com.connectrpc.protocols

import com.connectrpc.Code
import com.connectrpc.ConnectError
import com.connectrpc.Headers
import com.connectrpc.Interceptor
import com.connectrpc.ProtocolClientConfig
import com.connectrpc.StreamFunction
import com.connectrpc.StreamResult
import com.connectrpc.UnaryFunction
import com.connectrpc.compression.CompressionPool
import com.connectrpc.http.HTTPResponse
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
                val headers = response.headers
                val trailers = response.trailers
                val completion = completionParser.parse(headers, trailers)
                val code = completion?.code ?: Code.UNKNOWN
                if (response.code != Code.OK) {
                    return@UnaryFunction HTTPResponse(
                        code = response.code,
                        headers = headers,
                        message = Buffer(),
                        trailers = trailers,
                        error = response.error,
                        tracingInfo = response.tracingInfo
                    )
                }
                val compressionPool =
                    clientConfig.compressionPool(headers[GRPC_ENCODING]?.first())
                if (code == Code.OK) {
                    val (_, message) = Envelope.unpackWithHeaderByte(
                        response.message.buffer,
                        compressionPool
                    )
                    HTTPResponse(
                        code = code,
                        headers = headers,
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
                        headers = headers,
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
                        val headers = result.headers
                        val completion = completionParser.parse(headers, emptyMap())
                        if (completion != null) {
                            val connectError = grpcCompletionToConnectError(completion, serializationStrategy, result.error)
                            return@fold StreamResult.Complete(
                                code = connectError?.code ?: Code.OK,
                                error = connectError,
                                trailers = headers
                            )
                        }
                        responseCompressionPool = clientConfig.compressionPool(headers[GRPC_ENCODING]?.first())
                        StreamResult.Headers(headers)
                    },
                    onMessage = { result ->
                        val (_, unpackedMessage) = Envelope.unpackWithHeaderByte(
                            result.message,
                            responseCompressionPool
                        )
                        StreamResult.Message(unpackedMessage)
                    },
                    onCompletion = { result ->
                        val trailers = result.trailers
                        val completion = completionParser.parse(emptyMap(), trailers)
                        val connectError = grpcCompletionToConnectError(completion, serializationStrategy, result.error)
                        StreamResult.Complete(
                            code = connectError?.code ?: Code.OK,
                            error = connectError,
                            trailers = trailers
                        )
                    }
                )
                streamResult
            }
        )
    }

    private fun Headers.withGRPCRequestHeaders(): Headers {
        val headers = toMutableMap()
        if (headers.keys.none { it.equals(USER_AGENT, ignoreCase = true) }) {
            // https://github.com/grpc/grpc/blob/master/doc/PROTOCOL-HTTP2.md#user-agents
            headers[USER_AGENT] = listOf("grpc-kotlin-connect/${ConnectConstants.VERSION}")
        }
        headers[GRPC_TE_HEADER] = listOf("trailers")
        val requestCompression = clientConfig.requestCompression
        if (requestCompression != null) {
            headers[GRPC_ENCODING] = listOf(requestCompression.compressionPool.name())
        }
        return headers
    }
}

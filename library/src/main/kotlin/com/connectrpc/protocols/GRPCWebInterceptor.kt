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

package com.connectrpc.protocols

import com.connectrpc.Code
import com.connectrpc.ConnectError
import com.connectrpc.Headers
import com.connectrpc.Interceptor
import com.connectrpc.ProtocolClientConfig
import com.connectrpc.StreamFunction
import com.connectrpc.StreamResult
import com.connectrpc.Trailers
import com.connectrpc.UnaryFunction
import com.connectrpc.compression.CompressionPool
import com.connectrpc.http.HTTPResponse
import okio.Buffer

internal const val TRAILERS_BIT = 0b10000000

/**
 * The gRPC Web implementation.
 * https://github.com/grpc/grpc/blob/master/doc/PROTOCOL-WEB.md
 */
internal class GRPCWebInterceptor(
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
                val requestCompressionPool = clientConfig.requestCompression
                val requestMessage = Buffer().use { buffer ->
                    if (request.message != null) {
                        buffer.write(request.message)
                    }
                    buffer
                }
                // GRPC unary payloads are enveloped.
                val envelopedMessage = Envelope.pack(
                    requestMessage,
                    requestCompressionPool?.compressionPool,
                    requestCompressionPool?.minBytes
                )

                request.clone(
                    url = request.url,
                    // The underlying content type is overridden here.
                    contentType = "application/grpc-web+${serializationStrategy.serializationName()}",
                    headers = requestHeaders.withGRPCRequestHeaders(),
                    message = envelopedMessage.readByteArray()
                )
            },
            responseFunction = { response ->
                if (response.code != Code.OK) {
                    return@UnaryFunction HTTPResponse(
                        code = response.code,
                        headers = response.headers.toMutableMap(),
                        message = Buffer(),
                        trailers = emptyMap(),
                        error = response.error,
                        tracingInfo = response.tracingInfo
                    )
                }
                val responseHeaders = response.headers.toMutableMap()
                val compressionPool =
                    clientConfig.compressionPool(responseHeaders[GRPC_ENCODING]?.first())
                // gRPC Web returns data in 2 chunks (either/both of which may be compressed):
                // 1. OPTIONAL (when not trailers-only): The (headers and length prefixed)
                //    message data.
                // 2. The (headers and length prefixed) trailers data.
                // Reference:
                // https://github.com/grpc/grpc/blob/master/doc/PROTOCOL-WEB.md
                if (response.message.exhausted()) {
                    // There was no response body. Read trailers within the headers.
                    val trailers = response.headers
                    val completion = completionParser.parse(trailers)
                    val code = completion?.code ?: response.code
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
                } else {
                    // Unpack the current message and trailers.
                    val responseBuffer = response.message.buffer
                    val currentMessage = Buffer()
                    val header = responseBuffer.readByte()
                    val length = responseBuffer.readInt()
                    currentMessage.writeByte(header.toInt())
                    currentMessage.writeInt(length)
                    currentMessage.write(responseBuffer, length.toLong())
                    // currentMessage will contain remaining bytes unread by unpackWithHeaderByte.
                    val (headerByte, unpacked) = Envelope.unpackWithHeaderByte(
                        currentMessage,
                        compressionPool
                    )
                    // Check if the current message contains only trailers.
                    val trailerBuffer = if (headerByte.and(TRAILERS_BIT) == TRAILERS_BIT) {
                        unpacked
                    } else {
                        // The previous chunk is the message which means this is the trailers.
                        val (_, trailerBuffer) = Envelope.unpackWithHeaderByte(
                            responseBuffer,
                            compressionPool
                        )
                        trailerBuffer
                    }
                    val finalTrailers = parseGrpcWebTrailer(trailerBuffer)
                    val completionWithMessage = completionParser.parse(finalTrailers)
                    val finalCode = completionWithMessage?.code ?: Code.UNKNOWN
                    val error = if (finalCode != Code.OK && completionWithMessage != null) {
                        val result = Buffer()
                        val errorMessage = completionWithMessage.message
                        result.write(errorMessage)
                        ConnectError(
                            code = finalCode,
                            errorDetailParser = serializationStrategy.errorDetailParser(),
                            message = errorMessage.utf8(),
                            details = completionWithMessage.errorDetails
                        )
                    } else {
                        null
                    }
                    HTTPResponse(
                        code = finalCode,
                        headers = responseHeaders,
                        message = unpacked,
                        trailers = finalTrailers,
                        error = error,
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
                    contentType = "application/grpc-web+${serializationStrategy.serializationName()}",
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
                        // Trailers are passed in the headers for GRPC.
                        val streamTrailers: Trailers = responseHeaders
                        val completion = completionParser.parse(streamTrailers)
                        if (completion != null) {
                            val error = if (completion.code != Code.OK) {
                                ConnectError(
                                    code = completion.code,
                                    errorDetailParser = serializationStrategy.errorDetailParser(),
                                    message = completion.message.utf8(),
                                    details = completion.errorDetails,
                                    metadata = streamTrailers
                                )
                            } else {
                                null
                            }
                            return@fold StreamResult.Complete(
                                code = completion.code,
                                error = error,
                                trailers = responseHeaders
                            )
                        }
                        StreamResult.Headers(responseHeaders)
                    },
                    onMessage = { result ->
                        val (headerByte, unpackedMessage) = Envelope.unpackWithHeaderByte(
                            result.message,
                            responseCompressionPool
                        )
                        if (headerByte.and(TRAILERS_BIT) == TRAILERS_BIT) {
                            val streamTrailers = parseGrpcWebTrailer(unpackedMessage)
                            val completion = completionParser.parse(streamTrailers)
                            val code = completion!!.code
                            val connectError = if (result.connectError() != null) {
                                result.connectError()
                            } else if (result.error != null || code != Code.OK) {
                                ConnectError(
                                    code = code,
                                    errorDetailParser = serializationStrategy.errorDetailParser(),
                                    message = completion.message.utf8(),
                                    exception = result.error,
                                    details = completion.errorDetails,
                                    metadata = streamTrailers
                                )
                            } else {
                                // Successful call.
                                null
                            }
                            return@fold StreamResult.Complete(
                                code = code,
                                error = connectError,
                                trailers = streamTrailers
                            )
                        }
                        StreamResult.Message(unpackedMessage)
                    },
                    onCompletion = { result ->
                        result
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

    private fun parseGrpcWebTrailer(buffer: Buffer): Trailers {
        val trailers = mutableMapOf<String, List<String>>()
        val lines = buffer.readUtf8().split("\r\n")
        for (line in lines) {
            if (line == "") {
                continue
            }
            val i = line.indexOf(":")
            if (i > 0) {
                val name = line.substring(0, i).trim().lowercase()
                val value = line.substring(i + 1).trim().lowercase()
                trailers.put(name.lowercase(), listOf(value))
            }
        }
        return trailers
    }
}

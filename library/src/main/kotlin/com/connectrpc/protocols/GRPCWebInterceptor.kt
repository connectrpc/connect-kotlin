// Copyright 2022-2025 The Connect Authors
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
import com.connectrpc.ConnectException
import com.connectrpc.Headers
import com.connectrpc.Interceptor
import com.connectrpc.ProtocolClientConfig
import com.connectrpc.StreamFunction
import com.connectrpc.StreamResult
import com.connectrpc.Trailers
import com.connectrpc.UnaryFunction
import com.connectrpc.compression.CompressionPool
import com.connectrpc.http.clone
import okio.Buffer
import kotlin.time.Duration

/**
 * The gRPC Web implementation.
 * https://github.com/grpc/grpc/blob/master/doc/PROTOCOL-WEB.md
 */
internal class GRPCWebInterceptor(
    private val clientConfig: ProtocolClientConfig,
) : Interceptor {
    companion object {
        internal const val TRAILERS_BIT = 0b10000000
    }

    private val serializationStrategy = clientConfig.serializationStrategy
    private val completionParser = GRPCCompletionParser(serializationStrategy.errorDetailParser())
    private var responseCompressionPool: CompressionPool? = null
    private var responseHeaders: Headers = emptyMap()
    private var streamEmpty: Boolean = true

    override fun unaryFunction(): UnaryFunction {
        return UnaryFunction(
            requestFunction = { request ->
                val requestHeaders = request.headers.withGRPCRequestHeaders(request.timeout)
                val requestCompressionPool = clientConfig.requestCompression
                // GRPC unary payloads are enveloped.
                val envelopedMessage = Envelope.pack(
                    request.message,
                    requestCompressionPool?.compressionPool,
                    requestCompressionPool?.minBytes,
                )
                request.clone(
                    url = request.url,
                    // The underlying content type is overridden here.
                    contentType = "application/grpc-web+${serializationStrategy.serializationName()}",
                    headers = requestHeaders,
                    message = envelopedMessage,
                )
            },
            responseFunction = { response ->
                if (response.cause != null) {
                    return@UnaryFunction response.clone(message = Buffer())
                }
                val headers = response.headers
                if (response.status != 200) {
                    return@UnaryFunction response.clone(
                        message = Buffer(),
                        cause = ConnectException(
                            code = Code.fromHTTPStatus(response.status),
                            message = "unexpected status code: ${response.status}",
                            metadata = headers,
                        ),
                    )
                }
                val contentType = headers[CONTENT_TYPE]?.first() ?: ""
                if (!contentTypeIsExpectedGRPCWeb(contentType, serializationStrategy.serializationName())) {
                    // If content-type looks like it could be a gRPC server's response, consider
                    // this an internal error. Otherwise, we infer a code from the HTTP status,
                    // which means a code of UNKNOWN since HTTP status is 200.
                    val code = if (contentTypeIsGRPCWeb(contentType)) Code.INTERNAL_ERROR else Code.UNKNOWN
                    return@UnaryFunction response.clone(
                        message = Buffer(),
                        cause = ConnectException(
                            code = code,
                            message = "unexpected content-type: $contentType",
                            metadata = headers,
                        ),
                    )
                }
                val compressionPool =
                    clientConfig.compressionPool(headers[GRPC_ENCODING]?.first())
                // gRPC Web returns data in 2 chunks (either/both of which may be compressed):
                // 1. OPTIONAL (when not trailers-only): The (headers and length prefixed)
                //    message data.
                // 2. The (headers and length prefixed) trailers data.
                // Reference:
                // https://github.com/grpc/grpc/blob/master/doc/PROTOCOL-WEB.md
                if (response.message.exhausted()) {
                    // There was no response body. Read status within the headers.
                    var exception = completionParser
                        .parse(headers, false, emptyMap())
                        .toConnectExceptionOrNull(serializationStrategy)
                    if (exception == null) {
                        // No response data and no error code?
                        exception = ConnectException(
                            code = Code.UNIMPLEMENTED,
                            message = "unary stream has no messages",
                            metadata = headers,
                        )
                    }
                    response.clone(
                        message = Buffer(),
                        cause = exception,
                        trailers = headers,
                    )
                } else {
                    // Unpack the current message and trailers.
                    val responseBuffer = response.message.buffer
                    val (headerByte, unpacked) = Envelope.unpackWithHeaderByte(
                        responseBuffer,
                        compressionPool,
                    )
                    // Check if the current message contains only trailers.
                    val (currentMessage, trailerBuffer) = if (headerByte.and(TRAILERS_BIT) == TRAILERS_BIT) {
                        null to unpacked
                    } else if (response.message.exhausted()) {
                        return@UnaryFunction response.clone(
                            message = Buffer(),
                            cause = ConnectException(
                                code = Code.INTERNAL_ERROR,
                                message = "response did not include an end of stream message",
                                metadata = headers,
                            ),
                        )
                    } else {
                        // The previous chunk is the message which means this is the trailers.
                        val (trailerHeaderByte, trailerBuffer) = Envelope.unpackWithHeaderByte(
                            responseBuffer,
                            compressionPool,
                        )
                        if (trailerHeaderByte.and(TRAILERS_BIT) != TRAILERS_BIT) {
                            // Another message instead of trailers?
                            return@UnaryFunction response.clone(
                                message = Buffer(),
                                cause = ConnectException(
                                    code = Code.UNIMPLEMENTED,
                                    message = "unary stream has multiple messages",
                                    metadata = headers,
                                ),
                            )
                        }
                        unpacked to trailerBuffer
                    }
                    if (!responseBuffer.exhausted()) {
                        // More after the trailers message?
                        return@UnaryFunction response.clone(
                            message = Buffer(),
                            cause = ConnectException(
                                code = Code.INTERNAL_ERROR,
                                message = "response stream contains data after end-of-stream message",
                                metadata = headers,
                            ),
                        )
                    }
                    val finalTrailers = parseGrpcWebTrailer(trailerBuffer)
                    var exception = completionParser
                        .parse(headers, true, finalTrailers)
                        .toConnectExceptionOrNull(serializationStrategy)
                    if (exception == null && currentMessage == null) {
                        // No response message, and trailers indicated no error?
                        exception = ConnectException(
                            code = Code.UNIMPLEMENTED,
                            message = "unary stream has multiple messages",
                            metadata = headers,
                        )
                    }
                    response.clone(
                        message = currentMessage ?: Buffer(),
                        trailers = finalTrailers,
                        cause = exception,
                    )
                }
            },
        )
    }

    override fun streamFunction(): StreamFunction {
        return StreamFunction(
            requestFunction = { request ->
                request.clone(
                    url = request.url,
                    contentType = "application/grpc-web+${serializationStrategy.serializationName()}",
                    headers = request.headers.withGRPCRequestHeaders(request.timeout),
                )
            },
            requestBodyFunction = { buffer ->
                val requestCompression = clientConfig.requestCompression
                Envelope.pack(buffer, requestCompression?.compressionPool, requestCompression?.minBytes)
            },
            streamResultFunction = { res ->
                val streamResult = res.fold(
                    onHeaders = { result ->
                        responseHeaders = result.headers
                        val contentType = responseHeaders[CONTENT_TYPE]?.first() ?: ""
                        if (!contentTypeIsExpectedGRPCWeb(contentType, serializationStrategy.serializationName())) {
                            // If content-type looks like it could be a gRPC server's response, consider
                            // this an internal error. Otherwise, we infer a code from the HTTP status,
                            // which means a code of UNKNOWN since HTTP status is 200.
                            val code = if (contentTypeIsGRPCWeb(contentType)) Code.INTERNAL_ERROR else Code.UNKNOWN
                            StreamResult.Complete(
                                cause = ConnectException(
                                    code = code,
                                    message = "unexpected content-type: $contentType",
                                    metadata = responseHeaders,
                                ),
                            )
                        } else {
                            responseCompressionPool = clientConfig
                                .compressionPool(responseHeaders[GRPC_ENCODING]?.first())
                            StreamResult.Headers(responseHeaders)
                        }
                    },
                    onMessage = { result ->
                        streamEmpty = false
                        val (headerByte, unpackedMessage) = Envelope.unpackWithHeaderByte(
                            result.message,
                            responseCompressionPool,
                        )
                        if (headerByte.and(TRAILERS_BIT) == TRAILERS_BIT) {
                            val streamTrailers = parseGrpcWebTrailer(unpackedMessage)
                            val exception = completionParser
                                .parse(responseHeaders, !streamEmpty, streamTrailers)
                                .toConnectExceptionOrNull(serializationStrategy)
                            StreamResult.Complete(
                                cause = exception,
                                trailers = streamTrailers,
                            )
                        } else {
                            StreamResult.Message(unpackedMessage)
                        }
                    },
                    onCompletion = { result -> result },
                )
                streamResult
            },
        )
    }

    private fun Headers.withGRPCRequestHeaders(timeout: Duration?): Headers {
        // TODO: This is only slightly different from the version in GRPCInterceptor. Consolidate?
        val headers = toMutableMap()
        if (headers.keys.none { it.equals(GRPC_WEB_USER_AGENT, ignoreCase = true) }) {
            headers[GRPC_WEB_USER_AGENT] = listOf("grpc-kotlin-connect/${ConnectConstants.VERSION}")
        }
        if (timeout != null) {
            headers[GRPC_TIMEOUT] = listOf(grpcTimeoutString(timeout))
        }
        val requestCompression = clientConfig.requestCompression
        if (requestCompression != null) {
            headers[GRPC_ENCODING] = listOf(requestCompression.compressionPool.name())
        }
        if (clientConfig.compressionPools().isNotEmpty()) {
            headers[GRPC_ACCEPT_ENCODING] = clientConfig.compressionPools()
                .map { compressionPool -> compressionPool.name() }
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
                val name = line.substring(0, i).trim()
                val value = line.substring(i + 1).trim()
                trailers.compute(name.lowercase()) { _: String, a: List<String>? ->
                    if (a == null) {
                        mutableListOf(value)
                    } else {
                        (a as MutableList).add(value)
                        a
                    }
                }
            }
        }
        return trailers
    }
}

internal fun contentTypeIsGRPCWeb(contentType: String): Boolean {
    return contentType == "application/grpc-web" || contentType.startsWith("application/grpc-web+")
}

internal fun contentTypeIsExpectedGRPCWeb(contentType: String, expectCodec: String): Boolean {
    return (expectCodec == "proto" && contentType == "application/grpc-web") ||
        contentType == "application/grpc-web+$expectCodec"
}

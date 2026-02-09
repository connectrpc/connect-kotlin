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
import com.connectrpc.UnaryFunction
import com.connectrpc.compression.CompressionPool
import com.connectrpc.http.clone
import okio.Buffer
import kotlin.time.Duration

/**
 * The gRPC HTTP implementation
 * https://github.com/grpc/grpc/blob/master/doc/PROTOCOL-HTTP2.md
 */
internal class GRPCInterceptor(
    private val clientConfig: ProtocolClientConfig,
) : Interceptor {
    private val serializationStrategy = clientConfig.serializationStrategy
    private val completionParser = GRPCCompletionParser(serializationStrategy.errorDetailParser())
    private var responseCompressionPool: CompressionPool? = null
    private var responseHeaders: Headers = emptyMap()
    private var streamEmpty: Boolean = true

    override fun unaryFunction(): UnaryFunction {
        return UnaryFunction(
            requestFunction = { request ->
                val requestHeaders = request.headers.withGRPCRequestHeaders(request.timeout)
                val requestCompression = clientConfig.requestCompression
                // GRPC unary payloads are enveloped.
                val envelopedMessage = Envelope.pack(
                    request.message,
                    requestCompression?.compressionPool,
                    requestCompression?.minBytes,
                )
                request.clone(
                    url = request.url,
                    // The underlying content type is overridden here.
                    contentType = "application/grpc+${serializationStrategy.serializationName()}",
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
                if (!contentTypeIsExpectedGRPC(contentType, serializationStrategy.serializationName())) {
                    // If content-type looks like it could be a gRPC server's response, consider
                    // this an internal error. Otherwise, we infer a code from the HTTP status,
                    // which means a code of UNKNOWN since HTTP status is 200.
                    val code = if (contentTypeIsGRPC(contentType)) Code.INTERNAL_ERROR else Code.UNKNOWN
                    return@UnaryFunction response.clone(
                        message = Buffer(),
                        cause = ConnectException(
                            code = code,
                            message = "unexpected content-type: $contentType",
                            metadata = headers,
                        ),
                    )
                }
                var trailers = response.trailers
                val hasBody = !response.message.buffer.exhausted()
                val completion = completionParser.parse(headers, hasBody, trailers)
                if (completion.trailersOnly) {
                    trailers = headers // report the headers also as trailers
                }
                val exception = completion.toConnectExceptionOrNull(serializationStrategy)
                val message = if (exception == null) {
                    if (!hasBody) {
                        return@UnaryFunction response.clone(
                            message = Buffer(),
                            cause = ConnectException(
                                code = Code.UNIMPLEMENTED,
                                message = "unary stream has no messages",
                                metadata = headers.plus(trailers),
                            ),
                        )
                    }
                    val compressionPool =
                        clientConfig.compressionPool(headers[GRPC_ENCODING]?.first())
                    val (_, buffer) = Envelope.unpackWithHeaderByte(
                        response.message.buffer,
                        compressionPool,
                    )
                    if (!response.message.buffer.exhausted()) {
                        return@UnaryFunction response.clone(
                            message = Buffer(),
                            cause = ConnectException(
                                code = Code.UNIMPLEMENTED,
                                message = "unary stream has multiple messages",
                                metadata = headers.plus(trailers),
                            ),
                        )
                    }
                    buffer
                } else {
                    Buffer()
                }
                response.clone(
                    message = message,
                    cause = exception,
                    trailers = trailers,
                )
            },
        )
    }

    override fun streamFunction(): StreamFunction {
        return StreamFunction(
            requestFunction = { request ->
                request.clone(
                    url = request.url,
                    contentType = "application/grpc+${serializationStrategy.serializationName()}",
                    headers = request.headers.withGRPCRequestHeaders(request.timeout),
                )
            },
            requestBodyFunction = { buffer ->
                val requestCompression = clientConfig.requestCompression
                Envelope.pack(buffer, requestCompression?.compressionPool, requestCompression?.minBytes)
            },
            streamResultFunction = { res ->
                res.fold(
                    onHeaders = { result ->
                        responseHeaders = result.headers
                        val contentType = responseHeaders[CONTENT_TYPE]?.first() ?: ""
                        if (!contentTypeIsExpectedGRPC(contentType, serializationStrategy.serializationName())) {
                            // If content-type looks like it could be a gRPC server's response, consider
                            // this an internal error. Otherwise, we infer a code from the HTTP status,
                            // which means a code of UNKNOWN since HTTP status is 200.
                            val code = if (contentTypeIsGRPC(contentType)) Code.INTERNAL_ERROR else Code.UNKNOWN
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
                        val (_, unpackedMessage) = Envelope.unpackWithHeaderByte(
                            result.message,
                            responseCompressionPool,
                        )
                        StreamResult.Message(unpackedMessage)
                    },
                    onCompletion = { result ->
                        if (result.cause != null) {
                            return@fold result
                        }
                        val trailers = result.trailers
                        val exception = completionParser
                            .parse(responseHeaders, !streamEmpty, trailers)
                            .toConnectExceptionOrNull(serializationStrategy)
                        StreamResult.Complete(
                            cause = exception,
                            trailers = trailers,
                        )
                    },
                )
            },
        )
    }

    private fun Headers.withGRPCRequestHeaders(timeout: Duration?): Headers {
        val headers = toMutableMap()
        if (headers.keys.none { it.equals(USER_AGENT, ignoreCase = true) }) {
            // https://github.com/grpc/grpc/blob/master/doc/PROTOCOL-HTTP2.md#user-agents
            headers[USER_AGENT] = listOf("grpc-kotlin-connect/${ConnectConstants.VERSION}")
        }
        headers[GRPC_TE_HEADER] = listOf("trailers")
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
}

internal fun grpcTimeoutString(timeout: Duration): String {
    val nanos = "${timeout.inWholeNanoseconds}"
    // Spec states that value may be at most 8 digits.
    // So we change adjust units until it fits. We'll also
    // adjust to make the timeout more compact if we can do
    // so without loss of precision.
    if (nanos.length <= 8 && !nanos.endsWith("000")) {
        return "${nanos}n"
    }
    val micros = "${timeout.inWholeMicroseconds}"
    if (micros.length <= 8 && !micros.endsWith("000")) {
        return "${micros}u"
    }
    val millis = "${timeout.inWholeMilliseconds}"
    if (millis.length <= 8 && !millis.endsWith("000")) {
        return "${millis}m"
    }
    val secs = "${timeout.inWholeSeconds}"
    if (secs.length <= 8) {
        return "${secs}S"
    }
    // Clamp to maximum number of seconds that fits in 8 digits.
    // Over 3 years, so in practice, despite being clamped, this
    // is effectively an unbounded timeout.
    return "99999999S"
}

internal fun contentTypeIsGRPC(contentType: String): Boolean {
    return contentType == "application/grpc" || contentType.startsWith("application/grpc+")
}

internal fun contentTypeIsExpectedGRPC(contentType: String, expectCodec: String): Boolean {
    return (expectCodec == "proto" && contentType == "application/grpc") ||
        contentType == "application/grpc+$expectCodec"
}

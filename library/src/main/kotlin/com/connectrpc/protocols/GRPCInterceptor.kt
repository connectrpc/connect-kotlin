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

    override fun unaryFunction(): UnaryFunction {
        return UnaryFunction(
            requestFunction = { request ->
                val requestHeaders = mutableMapOf<String, List<String>>()
                requestHeaders.putAll(request.headers)
                if (clientConfig.compressionPools().isNotEmpty()) {
                    requestHeaders[GRPC_ACCEPT_ENCODING] = clientConfig.compressionPools()
                        .map { compressionPool -> compressionPool.name() }
                }
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
                    headers = requestHeaders.withGRPCRequestHeaders(),
                    message = envelopedMessage,
                )
            },
            responseFunction = { response ->
                if (response.cause != null) {
                    return@UnaryFunction response.clone(message = Buffer())
                }
                if (response.status != 200) {
                    return@UnaryFunction response.clone(
                        message = Buffer(),
                        cause = ConnectException(
                            code = Code.fromHTTPStatus(response.status),
                            message = "unexpected status code: ${response.status}",
                        ),
                    )
                }
                val headers = response.headers
                val trailers = response.trailers
                val exception = completionParser
                    .parse(headers, trailers)
                    .toConnectExceptionOrNull(serializationStrategy)
                val message = if (exception == null) {
                    if (response.message.buffer.exhausted()) {
                        return@UnaryFunction response.clone(
                            message = Buffer(),
                            cause = ConnectException(
                                code = Code.UNIMPLEMENTED,
                                message = "unary stream has no messages",
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
                    headers = request.headers.withGRPCRequestHeaders(),
                )
            },
            requestBodyFunction = { buffer ->
                val requestCompression = clientConfig.requestCompression
                Envelope.pack(buffer, requestCompression?.compressionPool, requestCompression?.minBytes)
            },
            streamResultFunction = { res ->
                res.fold(
                    onHeaders = { result ->
                        val headers = result.headers
                        val completion = completionParser.parse(headers, emptyMap())
                        if (completion.present) {
                            StreamResult.Complete(
                                cause = completion.toConnectExceptionOrNull(serializationStrategy),
                                trailers = headers,
                            )
                        } else {
                            responseHeaders = headers
                            responseCompressionPool = clientConfig
                                .compressionPool(headers[GRPC_ENCODING]?.first())
                            StreamResult.Headers(headers)
                        }
                    },
                    onMessage = { result ->
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
                            .parse(responseHeaders, trailers)
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

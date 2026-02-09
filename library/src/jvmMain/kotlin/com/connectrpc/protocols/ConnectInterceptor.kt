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
import com.connectrpc.Codec
import com.connectrpc.ConnectErrorDetail
import com.connectrpc.ConnectException
import com.connectrpc.Headers
import com.connectrpc.Idempotency
import com.connectrpc.Interceptor
import com.connectrpc.ProtocolClientConfig
import com.connectrpc.RequestCompression
import com.connectrpc.StreamFunction
import com.connectrpc.StreamResult
import com.connectrpc.StreamType
import com.connectrpc.Trailers
import com.connectrpc.UnaryFunction
import com.connectrpc.compression.CompressionPool
import com.connectrpc.http.HTTPMethod
import com.connectrpc.http.HTTPRequest
import com.connectrpc.http.UnaryHTTPRequest
import com.connectrpc.http.clone
import com.squareup.moshi.Moshi
import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.http.appendPathSegments
import io.ktor.http.encodedPath
import okio.Buffer
import okio.ByteString
import okio.ByteString.Companion.decodeBase64
import kotlin.time.Duration

/**
 * The Connect protocol.
 * https://connectrpc.com/docs
 */
internal class ConnectInterceptor(
    private val clientConfig: ProtocolClientConfig,
) : Interceptor {
    companion object {
        internal const val TRAILERS_BIT = 0b10
    }

    private val moshi = Moshi.Builder().build()
    private val serializationStrategy = clientConfig.serializationStrategy
    private var responseCompressionPool: CompressionPool? = null
    private var responseHeaders: Headers = emptyMap()

    override fun unaryFunction(): UnaryFunction {
        return UnaryFunction(
            requestFunction = { request ->
                val requestHeaders =
                    mutableMapOf(CONNECT_PROTOCOL_VERSION_KEY to listOf(CONNECT_PROTOCOL_VERSION_VALUE))
                requestHeaders.putAll(request.headers)
                if (request.timeout != null) {
                    requestHeaders[CONNECT_TIMEOUT_MS] = listOf(connectTimeoutString(request.timeout))
                }
                if (clientConfig.compressionPools().isNotEmpty()) {
                    requestHeaders[ACCEPT_ENCODING] = clientConfig.compressionPools().map { compressionPool ->
                        compressionPool.name()
                    }
                }
                if (requestHeaders.keys.none { it.equals(USER_AGENT, ignoreCase = true) }) {
                    requestHeaders[USER_AGENT] = listOf("connect-kotlin/${ConnectConstants.VERSION}")
                }
                val requestCompression = clientConfig.requestCompression
                val finalRequestBody = if (requestCompression?.shouldCompress(request.message) == true) {
                    requestHeaders[CONTENT_ENCODING] = listOf(requestCompression.compressionPool.name())
                    requestCompression.compressionPool.compress(request.message)
                } else {
                    request.message
                }
                if (shouldUseGETRequest(request, finalRequestBody)) {
                    constructGETRequest(request, finalRequestBody, requestCompression)
                } else {
                    request.clone(
                        url = request.url,
                        contentType = request.contentType,
                        headers = requestHeaders,
                        methodSpec = request.methodSpec,
                        message = finalRequestBody,
                    )
                }
            },
            responseFunction = { response ->
                if (response.cause != null) {
                    return@UnaryFunction response
                }
                val trailers = mutableMapOf<String, List<String>>()
                trailers.putAll(response.headers.toTrailers())
                val headers =
                    response.headers.filter { entry -> !entry.key.startsWith("trailer-") }
                val compressionPool = clientConfig.compressionPool(headers[CONTENT_ENCODING]?.first())
                val responseBody = try {
                    compressionPool?.decompress(response.message.buffer) ?: response.message.buffer
                } catch (e: Exception) {
                    return@UnaryFunction response.clone(
                        message = Buffer(),
                        headers = headers,
                        trailers = trailers,
                        cause = ConnectException(
                            code = Code.INTERNAL_ERROR,
                            message = e.message,
                            exception = e,
                            metadata = headers.plus(trailers),
                        ),
                    )
                }
                val contentType = headers[CONTENT_TYPE]?.first() ?: ""
                val exception: ConnectException?
                val message: Buffer
                if (response.status != 200) {
                    exception = parseConnectUnaryException(response.status, contentType, headers.plus(trailers), responseBody)
                    // We've already read the response body to parse an error - don't read again.
                    message = Buffer()
                } else {
                    message = responseBody
                    val isValidContentType =
                        (serializationStrategy.serializationName() == "json" && contentTypeIsJSON(contentType)) ||
                            contentType == "application/" + serializationStrategy.serializationName()
                    if (isValidContentType) {
                        exception = null
                    } else {
                        // If content-type looks like it could be an RPC server's response, consider
                        // this an internal error. Otherwise, we infer a code from the HTTP status,
                        // which means a code of UNKNOWN since HTTP status is 200.
                        val code = if (contentType.startsWith("application/")) Code.INTERNAL_ERROR else Code.UNKNOWN
                        exception = ConnectException(
                            code = code,
                            message = "unexpected content-type: $contentType",
                            metadata = headers.plus(trailers),
                        )
                    }
                }
                response.clone(
                    message = message,
                    headers = headers,
                    trailers = trailers,
                    cause = exception,
                )
            },
        )
    }

    override fun streamFunction(): StreamFunction {
        val requestCompression = clientConfig.requestCompression
        return StreamFunction(
            requestFunction = { request ->
                val requestHeaders =
                    mutableMapOf(CONNECT_PROTOCOL_VERSION_KEY to listOf(CONNECT_PROTOCOL_VERSION_VALUE))
                requestHeaders.putAll(request.headers)
                if (request.timeout != null) {
                    requestHeaders[CONNECT_TIMEOUT_MS] = listOf(connectTimeoutString(request.timeout))
                }
                if (requestCompression != null) {
                    requestHeaders[CONNECT_STREAMING_CONTENT_ENCODING] =
                        listOf(requestCompression.compressionPool.name())
                }
                if (requestHeaders.keys.none { it.equals(USER_AGENT, ignoreCase = true) }) {
                    requestHeaders[USER_AGENT] = listOf("connect-kotlin/${ConnectConstants.VERSION}")
                }
                requestHeaders[CONNECT_STREAMING_ACCEPT_ENCODING] =
                    clientConfig.compressionPools().map { entry -> entry.name() }
                request.clone(
                    url = request.url,
                    contentType = request.contentType,
                    headers = requestHeaders,
                    methodSpec = request.methodSpec,
                )
            },
            requestBodyFunction = { buffer ->
                val compressionPool = requestCompression?.compressionPool
                Envelope.pack(buffer, compressionPool, requestCompression?.minBytes)
            },
            streamResultFunction = { res ->
                val streamResult: StreamResult<Buffer> = res.fold(
                    onHeaders = { result ->
                        responseHeaders = result.headers
                        val contentType = responseHeaders[CONTENT_TYPE]?.first() ?: ""
                        val isValidContentType = contentType == "application/connect+" + serializationStrategy.serializationName()
                        if (!isValidContentType) {
                            // If content-type looks like it could be an RPC server's response, consider
                            // this an internal error. Otherwise, we infer a code from the HTTP status,
                            // which means a code of UNKNOWN since HTTP status is 200.
                            val code = if (contentType.startsWith("application/connect+")) Code.INTERNAL_ERROR else Code.UNKNOWN
                            StreamResult.Complete(
                                ConnectException(
                                    code = code,
                                    message = "unexpected content-type: $contentType",
                                    metadata = responseHeaders,
                                ),
                            )
                        } else {
                            responseCompressionPool =
                                clientConfig.compressionPool(responseHeaders[CONNECT_STREAMING_CONTENT_ENCODING]?.first())
                            StreamResult.Headers(responseHeaders)
                        }
                    },
                    onMessage = { result ->
                        val (headerByte, unpackedMessage) = Envelope.unpackWithHeaderByte(
                            result.message,
                            responseCompressionPool,
                        )
                        if (headerByte.and(TRAILERS_BIT) == TRAILERS_BIT) {
                            parseConnectEndStream(responseHeaders, unpackedMessage)
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

    private fun shouldUseGETRequest(request: HTTPRequest, finalRequestBody: Buffer): Boolean {
        return request.methodSpec.streamType == StreamType.UNARY &&
            request.methodSpec.idempotency == Idempotency.NO_SIDE_EFFECTS &&
            clientConfig.getConfiguration.useGET(finalRequestBody)
    }

    private fun constructGETRequest(
        request: UnaryHTTPRequest,
        finalRequestBody: Buffer,
        requestCompression: RequestCompression?,
    ): UnaryHTTPRequest {
        val serializationStrategy = clientConfig.serializationStrategy
        val requestCodec = serializationStrategy.codec(request.methodSpec.requestClass)
        val url = constructURLForGETRequest(
            request,
            requestCodec,
            finalRequestBody,
            requestCompression,
        )
        return request.clone(
            url = url,
            contentType = "",
            headers = request.headers,
            methodSpec = request.methodSpec,
            httpMethod = HTTPMethod.GET,
        )
    }

    private fun parseConnectEndStream(headers: Headers, source: Buffer): StreamResult.Complete<Buffer> {
        val adapter = moshi.adapter(EndStreamResponseJSON::class.java).nonNull()
        return source.use { bufferedSource ->
            val errorJSON = bufferedSource.readUtf8()
            val endStreamResponseJSON = try {
                adapter.fromJson(errorJSON)
            } catch (e: Throwable) {
                return StreamResult.Complete(
                    ConnectException(
                        code = Code.UNKNOWN,
                        exception = e,
                    ),
                )
            }
            val metadata = endStreamResponseJSON!!.metadata?.toLowercase()
            if (endStreamResponseJSON.error == null) {
                return StreamResult.Complete(trailers = metadata.orEmpty())
            }
            val code = Code.fromName(endStreamResponseJSON.error.code)
            StreamResult.Complete(
                cause = ConnectException(
                    code = code,
                    message = endStreamResponseJSON.error.message,
                    metadata = headers.plus(metadata.orEmpty()),
                ).withErrorDetails(
                    serializationStrategy.errorDetailParser(),
                    parseErrorDetails(endStreamResponseJSON.error),
                ),
                trailers = metadata.orEmpty(),
            )
        }
    }

    private fun parseConnectUnaryException(httpStatus: Int?, contentType: String, metadata: Headers, source: Buffer?): ConnectException {
        val code = Code.fromHTTPStatus(httpStatus)
        if (source == null || !contentTypeIsJSON(contentType)) {
            return ConnectException(code, "unexpected status code: $httpStatus")
        }
        return source.use { bufferedSource ->
            val adapter = moshi.adapter(ErrorPayloadJSON::class.java).nonNull()
            val errorJSON = bufferedSource.readUtf8()
            val errorPayloadJSON = try {
                adapter.fromJson(errorJSON)
            } catch (e: Exception) {
                return ConnectException(code, errorJSON, e)
            }
            val errorDetails = parseErrorDetails(errorPayloadJSON!!)
            ConnectException(
                code = Code.fromName(errorPayloadJSON.code, code),
                message = errorPayloadJSON.message,
                metadata = metadata,
            ).withErrorDetails(
                serializationStrategy.errorDetailParser(),
                errorDetails,
            )
        }
    }

    private fun parseErrorDetails(
        jsonClass: ErrorPayloadJSON,
    ): List<ConnectErrorDetail> {
        val errorDetails = mutableListOf<ConnectErrorDetail>()
        for (detail in jsonClass.details.orEmpty()) {
            if (detail.type == null) {
                continue
            }
            errorDetails.add(
                ConnectErrorDetail(
                    detail.type,
                    detail.value?.decodeBase64() ?: ByteString.EMPTY,
                ),
            )
        }
        return errorDetails
    }

    private fun Headers.toTrailers(): Trailers {
        val trailers = mutableMapOf<String, List<String>>()
        for (entry in entries) {
            val newKey = entry.key.substringAfter("trailer-", "")
            if (newKey.isEmpty()) continue
            trailers[newKey] = entry.value
        }
        return trailers
    }

    private fun connectTimeoutString(timeout: Duration): String {
        val millis = "${timeout.inWholeMilliseconds}"
        // Spec states that value may be at most 10 digits. So we
        // clamp to largest 10-digit value.
        if (millis.length > 10) {
            // Nearly 4 months, so in practice, despite being clamped,
            // this is effectively an unbounded timeout.
            return "9999999999"
        }
        return millis
    }

    private fun constructURLForGETRequest(
        httpRequest: HTTPRequest,
        codec: Codec<*>,
        payload: Buffer,
        requestCompression: RequestCompression?,
    ): Url {
        // The httpRequest.url already contains the full path including methodSpec.path
        // (added by ProtocolClient.urlFromMethodSpec), so we just need to add query parameters.
        return URLBuilder(httpRequest.url).apply {
            if (requestCompression?.shouldCompress(payload) == true) {
                parameters.append(GETConstants.COMPRESSION_QUERY_PARAM_KEY, requestCompression.compressionPool.name())
            }
            parameters.append(GETConstants.MESSAGE_QUERY_PARAM_KEY, payload.readByteString().base64Url())
            parameters.append(GETConstants.BASE64_QUERY_PARAM_KEY, "1")
            parameters.append(GETConstants.ENCODING_QUERY_PARAM_KEY, codec.encodingName())
            parameters.append(GETConstants.CONNECT_VERSION_QUERY_PARAM_KEY, GETConstants.CONNECT_VERSION_QUERY_PARAM_VALUE)
        }.build()
    }

    private fun Headers.toLowercase(): Headers {
        return asSequence().groupingBy {
            it.key.lowercase()
        }.aggregate { _: String, accumulator: List<String>?, element: Map.Entry<String, List<String>>, _: Boolean ->
            accumulator?.plus(element.value) ?: element.value
        }
    }
}

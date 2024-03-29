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
import okio.Buffer
import okio.ByteString
import okio.ByteString.Companion.decodeBase64
import java.net.URL

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

    override fun unaryFunction(): UnaryFunction {
        return UnaryFunction(
            requestFunction = { request ->
                val requestHeaders =
                    mutableMapOf(CONNECT_PROTOCOL_VERSION_KEY to listOf(CONNECT_PROTOCOL_VERSION_VALUE))
                requestHeaders.putAll(request.headers)
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
                trailers.putAll(response.trailers)
                val responseHeaders =
                    response.headers.filter { entry -> !entry.key.startsWith("trailer-") }
                val compressionPool = clientConfig.compressionPool(responseHeaders[CONTENT_ENCODING]?.first())
                val responseBody = try {
                    compressionPool?.decompress(response.message.buffer) ?: response.message.buffer
                } catch (e: Exception) {
                    return@UnaryFunction response.clone(
                        message = Buffer(),
                        headers = responseHeaders,
                        trailers = trailers,
                        cause = ConnectException(
                            code = Code.INTERNAL_ERROR,
                            errorDetailParser = serializationStrategy.errorDetailParser(),
                            message = e.message,
                            exception = e,
                        ),
                    )
                }
                val exception: ConnectException?
                val message: Buffer
                if (response.status != 200) {
                    exception = parseConnectUnaryException(response.status, response.headers, responseBody)
                    // We've already read the response body to parse an error - don't read again.
                    message = Buffer()
                } else {
                    exception = null
                    message = responseBody
                }
                response.clone(
                    message = message,
                    headers = responseHeaders,
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
                if (requestCompression != null) {
                    requestHeaders[CONNECT_STREAMING_CONTENT_ENCODING] = listOf(requestCompression.compressionPool.name())
                }
                if (requestHeaders.keys.none { it.equals(USER_AGENT, ignoreCase = true) }) {
                    requestHeaders[USER_AGENT] = listOf("connect-kotlin/${ConnectConstants.VERSION}")
                }
                requestHeaders[CONNECT_STREAMING_ACCEPT_ENCODING] = clientConfig.compressionPools().map { entry -> entry.name() }
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
                        val responseHeaders =
                            result.headers.filter { entry -> !entry.key.startsWith("trailer-") }
                        responseCompressionPool =
                            clientConfig.compressionPool(responseHeaders[CONNECT_STREAMING_CONTENT_ENCODING]?.first())
                        StreamResult.Headers(responseHeaders)
                    },
                    onMessage = { result ->
                        val (headerByte, unpackedMessage) = Envelope.unpackWithHeaderByte(
                            result.message,
                            responseCompressionPool,
                        )
                        if (headerByte.and(TRAILERS_BIT) == TRAILERS_BIT) {
                            parseConnectEndStream(unpackedMessage)
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
        val url = getUrlFromMethodSpec(
            request,
            requestCodec,
            finalRequestBody,
            requestCompression,
        )
        return request.clone(
            url = url,
            contentType = "application/${requestCodec.encodingName()}",
            headers = request.headers,
            methodSpec = request.methodSpec,
            httpMethod = HTTPMethod.GET,
        )
    }

    private fun parseConnectEndStream(source: Buffer): StreamResult.Complete<Buffer> {
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
                    errorDetailParser = serializationStrategy.errorDetailParser(),
                    message = endStreamResponseJSON.error.message,
                    details = parseErrorDetails(endStreamResponseJSON.error),
                    metadata = metadata.orEmpty(),
                ),
            )
        }
    }

    private fun parseConnectUnaryException(httpStatus: Int?, headers: Headers, source: Buffer?): ConnectException {
        val code = Code.fromHTTPStatus(httpStatus)
        if (source == null) {
            return ConnectException(code, serializationStrategy.errorDetailParser(), "unexpected status code: $httpStatus")
        }
        return source.use { bufferedSource ->
            val adapter = moshi.adapter(ErrorPayloadJSON::class.java).nonNull()
            val errorJSON = bufferedSource.readUtf8()
            val errorPayloadJSON = try {
                adapter.fromJson(errorJSON)
            } catch (e: Exception) {
                return ConnectException(code, serializationStrategy.errorDetailParser(), errorJSON, e)
            }
            val errorDetails = parseErrorDetails(errorPayloadJSON!!)
            ConnectException(
                code = Code.fromName(errorPayloadJSON.code),
                errorDetailParser = serializationStrategy.errorDetailParser(),
                message = errorPayloadJSON.message,
                details = errorDetails,
                metadata = headers,
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

private fun getUrlFromMethodSpec(
    httpRequest: HTTPRequest,
    codec: Codec<*>,
    payload: Buffer,
    requestCompression: RequestCompression?,
): URL {
    val baseURL = httpRequest.url
    val methodSpec = httpRequest.methodSpec
    val params = mutableListOf<String>()
    if (requestCompression?.shouldCompress(payload) == true) {
        params.add("${GETConstants.COMPRESSION_QUERY_PARAM_KEY}=${requestCompression.compressionPool.name()}")
    }
    params.add("${GETConstants.MESSAGE_QUERY_PARAM_KEY}=${payload.readByteString().base64Url()}")
    params.add("${GETConstants.BASE64_QUERY_PARAM_KEY}=1")
    params.add("${GETConstants.ENCODING_QUERY_PARAM_KEY}=${codec.encodingName()}")
    params.add("${GETConstants.CONNECT_VERSION_QUERY_PARAM_KEY}=${GETConstants.CONNECT_VERSION_QUERY_PARAM_VALUE}")
    params.sort()
    val queryParams = params.joinToString("&")
    val baseURI = baseURL.toURI()
        .resolve("/${methodSpec.path}?$queryParams")
    return baseURI.toURL()
}

fun Headers.toLowercase(): Headers {
    return asSequence().groupingBy {
        it.key.lowercase()
    }.aggregate { _: String, accumulator: List<String>?, element: Map.Entry<String, List<String>>, _: Boolean ->
        accumulator?.plus(element.value) ?: element.value
    }
}

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
import build.buf.connect.ConnectErrorDetail
import build.buf.connect.Headers
import build.buf.connect.Interceptor
import build.buf.connect.ProtocolClientConfig
import build.buf.connect.StreamFunction
import build.buf.connect.StreamResult
import build.buf.connect.Trailers
import build.buf.connect.UnaryFunction
import build.buf.connect.compression.CompressionPool
import build.buf.connect.http.HTTPRequest
import build.buf.connect.http.HTTPResponse
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okio.Buffer
import okio.ByteString
import okio.ByteString.Companion.encodeUtf8

/**
 * The Connect protocol.
 * https://connect.build/docs
 */
internal class ConnectInterceptor(
    private val clientConfig: ProtocolClientConfig
) : Interceptor {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    private val serializationStrategy = clientConfig.serializationStrategy
    private var responseCompressionPool: CompressionPool? = null

    override fun unaryFunction(): UnaryFunction {
        return UnaryFunction(
            requestFunction = { request ->
                val requestHeaders =
                    mutableMapOf(CONNECT_PROTOCOL_VERSION_KEY to listOf(CONNECT_PROTOCOL_VERSION_VALUE))
                requestHeaders.putAll(request.headers)
                if (clientConfig.compressionPools().isNotEmpty()) {
                    requestHeaders.put(
                        ACCEPT_ENCODING,
                        clientConfig.compressionPools()
                            .map { compressionPool -> compressionPool.name() }
                    )
                }
                val requestCompression = clientConfig.requestCompression
                val requestMessage = Buffer().use { buffer ->
                    if (request.message != null) {
                        buffer.write(request.message)
                    }
                    buffer
                }
                val finalRequestBody =
                    if (requestCompression != null && requestCompression.shouldCompress(requestMessage)) {
                        requestHeaders.put(CONTENT_ENCODING, listOf(requestCompression.compressionPool.name()))
                        requestCompression.compressionPool.compress(requestMessage)
                    } else {
                        requestMessage
                    }
                HTTPRequest(
                    url = request.url,
                    contentType = request.contentType,
                    headers = requestHeaders,
                    message = finalRequestBody.readByteArray()
                )
            },
            responseFunction = { response ->
                val trailers = mutableMapOf<String, List<String>>()
                trailers.putAll(response.headers.toTrailers())
                trailers.putAll(response.trailers)
                val responseHeaders =
                    response.headers.filter { entry -> !entry.key.startsWith("trailer") }.toMutableMap()
                val compressionPool = clientConfig.compressionPool(responseHeaders[CONTENT_ENCODING]?.first())
                val (code, connectError) = if (response.code != Code.OK) {
                    val error = parseConnectUnaryError(code = response.code, response.headers, response.message.buffer)
                    error.code to error
                } else {
                    response.code to null
                }
                val message = compressionPool?.decompress(response.message.buffer) ?: response.message.buffer
                HTTPResponse(
                    code = code,
                    message = message,
                    headers = responseHeaders,
                    trailers = trailers,
                    error = response.error ?: connectError,
                    tracingInfo = response.tracingInfo
                )
            }
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
                    requestHeaders.put(
                        CONNECT_STREAMING_CONTENT_ENCODING,
                        listOf(requestCompression.compressionPool.name())
                    )
                }
                requestHeaders.put(
                    CONNECT_STREAMING_ACCEPT_ENCODING,
                    clientConfig.compressionPools().map { entry -> entry.name() }
                )
                HTTPRequest(
                    url = request.url,
                    contentType = request.contentType,
                    headers = requestHeaders,
                    message = request.message
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
                            result.headers.filter { entry -> !entry.key.startsWith("trailer") }.toMutableMap()
                        responseCompressionPool =
                            clientConfig.compressionPool(responseHeaders[CONNECT_STREAMING_CONTENT_ENCODING]?.first())
                        StreamResult.Headers(responseHeaders)
                    },
                    onMessage = { result ->
                        val (headerByte, unpackedMessage) = Envelope.unpackWithHeaderByte(
                            result.message,
                            responseCompressionPool
                        )
                        val isEndStream = headerByte.shr(1).and(1) == 1
                        if (isEndStream) {
                            parseConnectEndStream(unpackedMessage)
                        } else {
                            StreamResult.Message(unpackedMessage)
                        }
                    },
                    onCompletion = { result ->
                        val streamTrailers: Trailers = result.trailers
                        val error = result.connectError()
                        StreamResult.Complete(error?.code ?: Code.OK, error = error, streamTrailers)
                    }
                )
                streamResult
            }
        )
    }

    private fun parseConnectEndStream(source: Buffer): StreamResult.Complete<Buffer> {
        val adapter = moshi.adapter(EndStreamResponseJSON::class.java)
        return source.use { bufferedSource ->
            val errorJSON = bufferedSource.readUtf8()
            val endStreamResponseJSON = try {
                adapter.fromJson(errorJSON) ?: return StreamResult.Complete(Code.OK)
            } catch (e: Throwable) {
                return StreamResult.Complete(Code.UNKNOWN, e)
            }
            val metadata = endStreamResponseJSON.metadata?.mapKeys { entry -> entry.key.lowercase() }
            if (endStreamResponseJSON.error?.code == null) {
                return StreamResult.Complete(Code.OK, trailers = metadata ?: emptyMap())
            }
            val code = Code.fromName(endStreamResponseJSON.error.code)
            StreamResult.Complete(
                code = code,
                error = ConnectError(
                    code = code,
                    errorDetailParser = serializationStrategy.errorDetailParser(),
                    message = endStreamResponseJSON.error.message,
                    details = parseErrorDetails(endStreamResponseJSON.error),
                    metadata = metadata ?: emptyMap()
                )
            )
        }
    }

    private fun parseConnectUnaryError(code: Code, headers: Headers, source: Buffer?): ConnectError {
        if (source == null) {
            return ConnectError(code, serializationStrategy.errorDetailParser(), "empty error message from source")
        }
        return source.use { bufferedSource ->
            val adapter = moshi.adapter(ErrorPayloadJSON::class.java)
            val errorJSON = bufferedSource.readUtf8()
            val errorPayloadJSON = try {
                adapter.fromJson(errorJSON) ?: return ConnectError(
                    code,
                    serializationStrategy.errorDetailParser(),
                    errorJSON
                )
            } catch (e: Throwable) {
                return ConnectError(code, serializationStrategy.errorDetailParser(), errorJSON)
            }
            val errorDetails = parseErrorDetails(errorPayloadJSON)
            ConnectError(
                code = Code.fromName(errorPayloadJSON.code),
                errorDetailParser = serializationStrategy.errorDetailParser(),
                message = errorPayloadJSON.message,
                details = errorDetails,
                metadata = headers
            )
        }
    }

    private fun parseErrorDetails(
        jsonClass: ErrorPayloadJSON?
    ): List<ConnectErrorDetail> {
        val errorDetails = mutableListOf<ConnectErrorDetail>()
        for (detail in jsonClass?.details.orEmpty()) {
            if (detail.type == null) {
                continue
            }
            val payload = detail.value
            if (payload == null) {
                errorDetails.add(ConnectErrorDetail(detail.type, ByteString.EMPTY))
                continue
            }
            errorDetails.add(
                ConnectErrorDetail(
                    detail.type,
                    payload.encodeUtf8()
                )
            )
        }
        return errorDetails
    }
}

private fun Headers.toTrailers(): Trailers {
    val trailers = mutableMapOf<String, MutableList<String>>()
    for (pair in this.filter { entry -> entry.key.startsWith("trailer") }) {
        val key = pair.key.substringAfter("trailer-")
        if (trailers.containsKey(key)) {
            trailers[key]?.add(pair.value.first())
        } else {
            trailers[key] = mutableListOf(pair.value.first())
        }
    }
    return trailers
}

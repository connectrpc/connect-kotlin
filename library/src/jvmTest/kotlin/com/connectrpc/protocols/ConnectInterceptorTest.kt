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
import com.connectrpc.ConnectException
import com.connectrpc.ErrorDetailParser
import com.connectrpc.Idempotency
import com.connectrpc.MethodSpec
import com.connectrpc.ProtocolClientConfig
import com.connectrpc.RequestCompression
import com.connectrpc.SerializationStrategy
import com.connectrpc.StreamResult
import com.connectrpc.StreamType
import com.connectrpc.compression.GzipCompressionPool
import com.connectrpc.http.HTTPMethod
import com.connectrpc.http.HTTPRequest
import com.connectrpc.http.HTTPResponse
import com.connectrpc.http.UnaryHTTPRequest
import com.squareup.moshi.Moshi
import io.ktor.http.Url
import okio.Buffer
import okio.ByteString.Companion.encodeUtf8
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class ConnectInterceptorTest {
    private val errorDetailParser: ErrorDetailParser = mock { }
    private val serializationStrategy: SerializationStrategy = mock { }

    private val moshi = Moshi.Builder().build()

    @Before
    fun setup() {
        val codec: Codec<Any> = mock { }
        whenever(codec.encodingName()).thenReturn("encoding_name")
        whenever(serializationStrategy.codec(Any::class)).thenReturn(codec)
        whenever(serializationStrategy.errorDetailParser()).thenReturn(errorDetailParser)
        whenever(serializationStrategy.serializationName()).thenReturn("encoding_type")
    }

    /*
     * Unary
     */
    @Test
    fun requestHeaders() {
        val config = ProtocolClientConfig(
            host = "https://connectrpc.com",
            serializationStrategy = serializationStrategy,
            compressionPools = emptyList(),
        )
        val connectInterceptor = ConnectInterceptor(config)
        val unaryFunction = connectInterceptor.unaryFunction()

        val request = unaryFunction.requestFunction(
            UnaryHTTPRequest(
                url = Url(config.host),
                contentType = "content_type",
                timeout = 2.5.toDuration(DurationUnit.SECONDS),
                headers = mapOf("key" to listOf("value")),
                message = Buffer(),
                methodSpec = MethodSpec(
                    path = "",
                    requestClass = Any::class,
                    responseClass = Any::class,
                    streamType = StreamType.UNARY,
                ),
            ),
        )
        assertThat(request.headers[CONNECT_PROTOCOL_VERSION_KEY]).containsExactly(CONNECT_PROTOCOL_VERSION_VALUE)
        assertThat(request.headers[ACCEPT_ENCODING]).isNullOrEmpty()
        assertThat(request.headers[CONTENT_ENCODING]).isNullOrEmpty()
        assertThat(request.headers[CONNECT_TIMEOUT_MS]).containsExactly("2500")
        assertThat(request.headers["key"]).containsExactly("value")
        assertThat(request.contentType).isEqualTo("content_type")
        assertThat(request.headers[USER_AGENT]).containsExactly("connect-kotlin/dev")
    }

    @Test
    fun requestHeadersCustomUserAgent() {
        val config = ProtocolClientConfig(
            host = "https://buf.build",
            serializationStrategy = serializationStrategy,
            compressionPools = emptyList(),
        )
        val connectInterceptor = ConnectInterceptor(config)
        val unaryFunction = connectInterceptor.unaryFunction()

        val request = unaryFunction.requestFunction(
            UnaryHTTPRequest(
                url = Url(config.host),
                contentType = "content_type",
                timeout = null,
                headers = mapOf("User-Agent" to listOf("custom-user-agent")),
                message = Buffer(),
                methodSpec = MethodSpec(
                    path = "",
                    requestClass = Any::class,
                    responseClass = Any::class,
                    streamType = StreamType.UNARY,
                ),
            ),
        )
        // this will only work if we do a case-insensitive lookup of headers
        assertThat(request.headers[USER_AGENT]).isNull()
        assertThat(request.headers["User-Agent"]).containsExactly("custom-user-agent")
    }

    @Test
    fun uncompressedRequestMessage() {
        val config = ProtocolClientConfig(
            host = "https://connectrpc.com",
            serializationStrategy = serializationStrategy,
        )
        val connectInterceptor = ConnectInterceptor(config)
        val unaryFunction = connectInterceptor.unaryFunction()

        val request = unaryFunction.requestFunction(
            UnaryHTTPRequest(
                url = Url(config.host),
                contentType = "content_type",
                timeout = null,
                headers = emptyMap(),
                message = Buffer().write("message".encodeUtf8()),
                methodSpec = MethodSpec(
                    path = "",
                    requestClass = Any::class,
                    responseClass = Any::class,
                    streamType = StreamType.UNARY,
                ),
            ),
        )
        assertThat(request.message.readUtf8()).isEqualTo("message")
    }

    @Test
    fun compressedRequestMessage() {
        val config = ProtocolClientConfig(
            host = "https://connectrpc.com",
            serializationStrategy = serializationStrategy,
            requestCompression = RequestCompression(1, GzipCompressionPool),
            compressionPools = listOf(GzipCompressionPool),
        )
        val connectInterceptor = ConnectInterceptor(config)
        val unaryFunction = connectInterceptor.unaryFunction()

        val request = unaryFunction.requestFunction(
            UnaryHTTPRequest(
                url = Url(config.host),
                contentType = "content_type",
                timeout = null,
                headers = emptyMap(),
                message = Buffer().write("message".encodeUtf8()),
                methodSpec = MethodSpec(
                    path = "",
                    requestClass = Any::class,
                    responseClass = Any::class,
                    streamType = StreamType.UNARY,
                ),
            ),
        )
        val decompressed = GzipCompressionPool.decompress(request.message)
        assertThat(decompressed.readUtf8()).isEqualTo("message")
    }

    @Test
    fun compressedEmptyRequestMessage() {
        val config = ProtocolClientConfig(
            host = "https://connectrpc.com",
            serializationStrategy = serializationStrategy,
            requestCompression = RequestCompression(1, GzipCompressionPool),
            compressionPools = listOf(GzipCompressionPool),
        )
        val connectInterceptor = ConnectInterceptor(config)
        val unaryFunction = connectInterceptor.unaryFunction()

        val request = unaryFunction.requestFunction(
            UnaryHTTPRequest(
                url = Url(config.host),
                contentType = "content_type",
                timeout = null,
                headers = emptyMap(),
                message = Buffer(),
                methodSpec = MethodSpec(
                    path = "",
                    requestClass = Any::class,
                    responseClass = Any::class,
                    streamType = StreamType.UNARY,
                ),
            ),
        )
        val decompressed = GzipCompressionPool.decompress(request.message)
        assertThat(decompressed.readUtf8()).isEqualTo("")
    }

    @Test
    fun uncompressedResponseMessage() {
        val config = ProtocolClientConfig(
            host = "https://connectrpc.com",
            serializationStrategy = serializationStrategy,
            compressionPools = listOf(GzipCompressionPool),
        )
        val connectInterceptor = ConnectInterceptor(config)
        val unaryFunction = connectInterceptor.unaryFunction()

        val response = unaryFunction.responseFunction(
            HTTPResponse(
                status = 200,
                headers = emptyMap(),
                message = Buffer().write("message".encodeUtf8()),
                trailers = emptyMap(),
            ),
        )
        assertThat(response.message.readUtf8()).isEqualTo("message")
    }

    @Test
    fun compressedResponseMessage() {
        val config = ProtocolClientConfig(
            host = "https://connectrpc.com",
            serializationStrategy = serializationStrategy,
            compressionPools = listOf(GzipCompressionPool),
        )
        val connectInterceptor = ConnectInterceptor(config)
        val unaryFunction = connectInterceptor.unaryFunction()

        val response = unaryFunction.responseFunction(
            HTTPResponse(
                status = 200,
                headers = mapOf(CONTENT_ENCODING to listOf(GzipCompressionPool.name())),
                message = GzipCompressionPool.compress(Buffer().write("message".encodeUtf8())),
                trailers = emptyMap(),
            ),
        )
        assertThat(response.message.readUtf8()).isEqualTo("message")
    }

    @Test
    fun compressedEmptyResponseMessage() {
        val config = ProtocolClientConfig(
            host = "https://connectrpc.com",
            serializationStrategy = serializationStrategy,
            compressionPools = listOf(GzipCompressionPool),
        )
        val connectInterceptor = ConnectInterceptor(config)
        val unaryFunction = connectInterceptor.unaryFunction()

        val response = unaryFunction.responseFunction(
            HTTPResponse(
                status = 200,
                headers = mapOf(CONTENT_ENCODING to listOf(GzipCompressionPool.name())),
                message = Buffer(),
                trailers = emptyMap(),
            ),
        )
        assertThat(response.message.readUtf8()).isEqualTo("")
    }

    @Test
    fun responseError() {
        val config = ProtocolClientConfig(
            host = "https://connectrpc.com",
            serializationStrategy = serializationStrategy,
            compressionPools = listOf(GzipCompressionPool),
        )
        val connectInterceptor = ConnectInterceptor(config)
        val unaryFunction = connectInterceptor.unaryFunction()
        val error = ErrorPayloadJSON(
            "resource_exhausted",
            "no more resources!",
            listOf(
                ErrorDetailPayloadJSON(
                    "type",
                    "value".encodeUtf8().base64(),
                ),
            ),
        )
        val adapter = moshi.adapter(ErrorPayloadJSON::class.java)
        val json = adapter.toJson(error)

        val response = unaryFunction.responseFunction(
            HTTPResponse(
                // body contents override status code
                status = 503,
                headers = mapOf(CONTENT_TYPE to listOf("application/json; charset=utf-8")),
                message = Buffer().write(json.encodeUtf8()),
                trailers = emptyMap(),
            ),
        )
        assertThat(response.cause!!.code).isEqualTo(Code.RESOURCE_EXHAUSTED)
        assertThat(response.cause!!.message).isEqualTo("no more resources!")
        val connectErrorDetail = response.cause!!.details.singleOrNull()!!
        assertThat(connectErrorDetail.type).isEqualTo("type")
        assertThat(connectErrorDetail.payload).isEqualTo("value".encodeUtf8())
    }

    @Test
    fun responseErrorBadJSON() {
        val config = ProtocolClientConfig(
            host = "https://connectrpc.com",
            serializationStrategy = serializationStrategy,
            compressionPools = listOf(GzipCompressionPool),
        )
        val connectInterceptor = ConnectInterceptor(config)
        val unaryFunction = connectInterceptor.unaryFunction()

        val response = unaryFunction.responseFunction(
            HTTPResponse(
                status = 503,
                headers = emptyMap(),
                message = Buffer().write("garbage json".encodeUtf8()),
                trailers = emptyMap(),
            ),
        )
        assertThat(response.cause!!.code).isEqualTo(Code.UNAVAILABLE)
    }

    @Test
    fun tracingInfoForwardedOnUnaryResponse() {
        val config = ProtocolClientConfig(
            host = "https://connectrpc.com",
            serializationStrategy = serializationStrategy,
        )
        val unaryFunction = ConnectInterceptor(config).unaryFunction()

        val response = unaryFunction.responseFunction(
            HTTPResponse(
                status = null,
                headers = emptyMap(),
                message = Buffer(),
                trailers = emptyMap(),
                cause = ConnectException(code = Code.UNKNOWN),
            ),
        )
        assertThat(response.cause!!.code).isEqualTo(Code.UNKNOWN)
    }

    /*
     * Streaming
     */
    @Test
    fun streamingRequestHeadersWithCompression() {
        val config = ProtocolClientConfig(
            host = "https://connectrpc.com",
            serializationStrategy = serializationStrategy,
            requestCompression = RequestCompression(1000, GzipCompressionPool),
            compressionPools = listOf(GzipCompressionPool),
        )
        val connectInterceptor = ConnectInterceptor(config)
        val streamFunction = connectInterceptor.streamFunction()

        val request = streamFunction.requestFunction(
            HTTPRequest(
                url = Url(config.host),
                contentType = "content_type",
                timeout = null,
                headers = mapOf("key" to listOf("value")),
                methodSpec = MethodSpec(
                    path = "",
                    requestClass = Any::class,
                    responseClass = Any::class,
                    streamType = StreamType.BIDI,
                ),
            ),
        )
        assertThat(request.headers[CONNECT_PROTOCOL_VERSION_KEY]).containsExactly(CONNECT_PROTOCOL_VERSION_VALUE)
        assertThat(request.headers[CONNECT_STREAMING_ACCEPT_ENCODING]).containsExactly(GzipCompressionPool.name())
        assertThat(request.headers[CONNECT_STREAMING_CONTENT_ENCODING]).containsExactly(GzipCompressionPool.name())
        assertThat(request.headers["key"]).containsExactly("value")
        assertThat(request.contentType).isEqualTo("content_type")
        assertThat(request.headers[USER_AGENT]).containsExactly("connect-kotlin/dev")
    }

    @Test
    fun streamingRequestHeadersCustomUserAgent() {
        val config = ProtocolClientConfig(
            host = "https://buf.build",
            serializationStrategy = serializationStrategy,
            requestCompression = RequestCompression(1000, GzipCompressionPool),
            compressionPools = listOf(GzipCompressionPool),
        )
        val connectInterceptor = ConnectInterceptor(config)
        val streamFunction = connectInterceptor.streamFunction()

        val request = streamFunction.requestFunction(
            HTTPRequest(
                url = Url(config.host),
                contentType = "content_type",
                timeout = null,
                headers = mapOf("User-Agent" to listOf("custom-user-agent")),
                methodSpec = MethodSpec(
                    path = "",
                    requestClass = Any::class,
                    responseClass = Any::class,
                    streamType = StreamType.UNARY,
                ),
            ),
        )
        // this will only work if we do a case-insensitive lookup of headers
        assertThat(request.headers[USER_AGENT]).isNull()
        assertThat(request.headers["User-Agent"]).containsExactly("custom-user-agent")
    }

    @Test
    fun streamingRequestHeadersNoCompression() {
        val config = ProtocolClientConfig(
            host = "https://connectrpc.com",
            serializationStrategy = serializationStrategy,
            compressionPools = emptyList(),
        )
        val connectInterceptor = ConnectInterceptor(config)
        val streamFunction = connectInterceptor.streamFunction()

        val request = streamFunction.requestFunction(
            HTTPRequest(
                url = Url(config.host),
                contentType = "content_type",
                timeout = null,
                headers = mapOf("key" to listOf("value")),
                methodSpec = MethodSpec(
                    path = "",
                    requestClass = Any::class,
                    responseClass = Any::class,
                    streamType = StreamType.BIDI,
                ),
            ),
        )
        assertThat(request.headers[CONNECT_PROTOCOL_VERSION_KEY]).containsExactly(CONNECT_PROTOCOL_VERSION_VALUE)
        assertThat(request.headers[CONNECT_STREAMING_ACCEPT_ENCODING]).isNullOrEmpty()
        assertThat(request.headers[CONNECT_STREAMING_CONTENT_ENCODING]).isNullOrEmpty()
        assertThat(request.headers["key"]).containsExactly("value")
        assertThat(request.contentType).isEqualTo("content_type")
        assertThat(request.headers[USER_AGENT]).containsExactly("connect-kotlin/dev")
    }

    @Test
    fun uncompressedStreamingRequestMessage() {
        val config = ProtocolClientConfig(
            host = "https://connectrpc.com",
            serializationStrategy = serializationStrategy,
        )
        val connectInterceptor = ConnectInterceptor(config)
        val streamFunction = connectInterceptor.streamFunction()

        val envelopedMessage = streamFunction.requestBodyFunction(Buffer().write("hello".encodeUtf8()))
        val (_, requestMessage) = Envelope.unpackWithHeaderByte(envelopedMessage)
        assertThat(requestMessage.readUtf8()).isEqualTo("hello")
    }

    @Test
    fun compressedStreamingRequestMessage() {
        val config = ProtocolClientConfig(
            host = "https://connectrpc.com",
            serializationStrategy = serializationStrategy,
            compressionPools = listOf(GzipCompressionPool),
        )
        val connectInterceptor = ConnectInterceptor(config)
        val streamFunction = connectInterceptor.streamFunction()

        val envelopedMessage = streamFunction.requestBodyFunction(Buffer().write("hello".encodeUtf8()))
        val (_, requestMessage) = Envelope.unpackWithHeaderByte(envelopedMessage, GzipCompressionPool)
        assertThat(requestMessage.readUtf8()).isEqualTo("hello")
    }

    @Test
    fun streamingResponseHeaders() {
        val config = ProtocolClientConfig(
            host = "https://connectrpc.com",
            serializationStrategy = serializationStrategy,
            compressionPools = listOf(GzipCompressionPool),
        )
        val connectInterceptor = ConnectInterceptor(config)
        val streamFunction = connectInterceptor.streamFunction()

        val result = streamFunction.streamResultFunction(
            StreamResult.Headers(
                headers = mapOf(
                    "trailer-x-some-key" to listOf("some_value"),
                    CONTENT_TYPE to listOf("application/connect+encoding_type"),
                    CONNECT_STREAMING_CONTENT_ENCODING to listOf("gzip"),
                ),
            ),
        )

        assertThat(result).isInstanceOf(StreamResult.Headers::class.java)
        val headerResult = result as StreamResult.Headers
        assertThat(headerResult.headers).isEqualTo(
            mapOf(
                "trailer-x-some-key" to listOf("some_value"),
                CONTENT_TYPE to listOf("application/connect+encoding_type"),
                CONNECT_STREAMING_CONTENT_ENCODING to listOf("gzip"),
            ),
        )
    }

    @Test
    fun uncompressedStreamingResponseMessage() {
        val config = ProtocolClientConfig(
            host = "https://connectrpc.com",
            serializationStrategy = serializationStrategy,
            compressionPools = listOf(GzipCompressionPool),
        )
        val connectInterceptor = ConnectInterceptor(config)
        val streamFunction = connectInterceptor.streamFunction()
        // Send headers for no compression.
        streamFunction.streamResultFunction(StreamResult.Headers(emptyMap()))

        val envelopedMessage = Envelope.pack(Buffer().write("hello".encodeUtf8()))
        val result = streamFunction.streamResultFunction(
            StreamResult.Message(
                Buffer().write(envelopedMessage.readByteString()),
            ),
        )

        assertThat(result).isInstanceOf(StreamResult.Message::class.java)
        val streamMessage = result as StreamResult.Message
        assertThat(streamMessage.message.readUtf8()).isEqualTo("hello")
    }

    @Test
    fun compressedStreamingResponseMessage() {
        val config = ProtocolClientConfig(
            host = "https://connectrpc.com",
            serializationStrategy = serializationStrategy,
            requestCompression = RequestCompression(1, GzipCompressionPool),
            compressionPools = listOf(GzipCompressionPool),
        )
        val connectInterceptor = ConnectInterceptor(config)
        val streamFunction = connectInterceptor.streamFunction()
        // Send headers for gzip compression.
        streamFunction.streamResultFunction(
            StreamResult.Headers(
                headers = mapOf(
                    CONTENT_TYPE to listOf("application/connect+encoding_type"),
                    CONNECT_STREAMING_CONTENT_ENCODING to listOf("gzip"),
                ),
            ),
        )

        val envelopedMessage = Envelope.pack(Buffer().write("hello".encodeUtf8()), GzipCompressionPool, 1)
        val result = streamFunction.streamResultFunction(
            StreamResult.Message(
                Buffer().write(envelopedMessage.readByteString()),
            ),
        )

        assertThat(result).isInstanceOf(StreamResult.Message::class.java)
        val streamMessage = result as StreamResult.Message
        assertThat(streamMessage.message.readUtf8()).isEqualTo("hello")
    }

    @Test
    fun endStreamOnResponseMessage() {
        val config = ProtocolClientConfig(
            host = "https://connectrpc.com",
            serializationStrategy = serializationStrategy,
        )
        val connectInterceptor = ConnectInterceptor(config)
        val streamFunction = connectInterceptor.streamFunction()
        val error = ErrorPayloadJSON(
            "resource_exhausted",
            "no more resources!",
            listOf(
                ErrorDetailPayloadJSON(
                    "type",
                    "value".encodeUtf8().base64(),
                ),
            ),
        )
        val endStream = EndStreamResponseJSON(
            error = error,
            metadata = emptyMap(),
        )
        val adapter = moshi.adapter(EndStreamResponseJSON::class.java)
        val json = adapter.toJson(endStream)
        val endStreamMessage = Buffer()
        endStreamMessage.writeByte(ConnectInterceptor.TRAILERS_BIT)
        endStreamMessage.writeInt(json.length)
        endStreamMessage.write(json.encodeUtf8())

        val result = streamFunction.streamResultFunction(
            StreamResult.Message(
                Buffer().write(endStreamMessage.readByteString()),
            ),
        )
        assertThat(result).isInstanceOf(StreamResult.Complete::class.java)
        val completion = result as StreamResult.Complete
        val exception = completion.cause!!
        assertThat(exception.code).isEqualTo(Code.RESOURCE_EXHAUSTED)
        assertThat(exception.message).isEqualTo("no more resources!")
        val errorDetail = exception.details.single()
        assertThat(errorDetail.type).isEqualTo("type")
        assertThat(errorDetail.payload).isEqualTo("value".encodeUtf8())
    }

    @Test
    fun endStreamResponseBadJSON() {
        val config = ProtocolClientConfig(
            host = "https://connectrpc.com",
            serializationStrategy = serializationStrategy,
        )
        val connectInterceptor = ConnectInterceptor(config)
        val streamFunction = connectInterceptor.streamFunction()

        val badData = "some garbage json".encodeUtf8()
        val result = streamFunction.streamResultFunction(
            StreamResult.Message(
                Buffer().writeByte(ConnectInterceptor.TRAILERS_BIT)
                    .writeInt(badData.size)
                    .write(badData),
            ),
        )
        assertThat(result).isInstanceOf(StreamResult.Complete::class.java)
        val completion = result as StreamResult.Complete
        assertThat(completion.cause!!.code).isEqualTo(Code.UNKNOWN)
    }

    @Test
    fun endStreamOnTrailers() {
        val config = ProtocolClientConfig(
            host = "https://connectrpc.com",
            serializationStrategy = serializationStrategy,
        )
        val connectInterceptor = ConnectInterceptor(config)
        val streamFunction = connectInterceptor.streamFunction()

        val result = streamFunction.streamResultFunction(
            StreamResult.Complete(
                trailers = mapOf(
                    "key" to listOf("value"),
                ),
            ),
        )

        assertThat(result).isInstanceOf(StreamResult.Complete::class.java)
        val completion = result as StreamResult.Complete
        assertThat(completion.cause).isNull()
        assertThat(completion.trailers["key"]).containsExactly("value")
    }

    @Test
    fun endStreamForwardsErrors() {
        val config = ProtocolClientConfig(
            host = "https://connectrpc.com",
            serializationStrategy = serializationStrategy,
        )
        val connectInterceptor = ConnectInterceptor(config)
        val streamFunction = connectInterceptor.streamFunction()

        val result = streamFunction.streamResultFunction(
            StreamResult.Complete(
                cause = ConnectException(
                    Code.UNKNOWN,
                    message = "error_message",
                ),
            ),
        )

        assertThat(result).isInstanceOf(StreamResult.Complete::class.java)
        val completion = result as StreamResult.Complete
        assertThat(completion.cause!!.code).isEqualTo(Code.UNKNOWN)
    }

    @Test
    fun getRequestCreatedForPayloadUnderLimit() {
        val config = ProtocolClientConfig(
            host = "https://connectrpc.com",
            serializationStrategy = serializationStrategy,
            compressionPools = emptyList(),
            getConfiguration = GETConfiguration.EnabledWithFallback(
                maxMessageBytes = 10_000,
            ),
        )
        val connectInterceptor = ConnectInterceptor(config)
        val unaryFunction = connectInterceptor.unaryFunction()

        val request = unaryFunction.requestFunction(
            UnaryHTTPRequest(
                url = Url(config.host),
                contentType = "content_type",
                timeout = null,
                headers = mapOf("key" to listOf("value")),
                message = Buffer().write(ByteArray(5_000)),
                methodSpec = MethodSpec(
                    path = "",
                    requestClass = Any::class,
                    responseClass = Any::class,
                    streamType = StreamType.UNARY,
                    idempotency = Idempotency.NO_SIDE_EFFECTS,
                ),
            ),
        )

        val queryMap = parseQuery(request)
        assertThat(queryMap.get(GETConstants.MESSAGE_QUERY_PARAM_KEY)).isNotNull()
        assertThat(queryMap.get(GETConstants.BASE64_QUERY_PARAM_KEY)).isEqualTo("1")
        assertThat(queryMap.get(GETConstants.ENCODING_QUERY_PARAM_KEY)).isEqualTo("encoding_name")
        assertThat(queryMap.get(GETConstants.CONNECT_VERSION_QUERY_PARAM_KEY)).isEqualTo("v1")
        assertThat(request.httpMethod).isEqualTo(HTTPMethod.GET)
    }

    @Test
    fun fallbackRequestCreatedForPayloadOverLimit() {
        val config = ProtocolClientConfig(
            host = "https://connectrpc.com",
            serializationStrategy = serializationStrategy,
            compressionPools = emptyList(),
            getConfiguration = GETConfiguration.EnabledWithFallback(
                maxMessageBytes = 1,
            ),
        )
        val connectInterceptor = ConnectInterceptor(config)
        val unaryFunction = connectInterceptor.unaryFunction()

        val request = unaryFunction.requestFunction(
            UnaryHTTPRequest(
                url = Url(config.host),
                contentType = "content_type",
                timeout = null,
                headers = mapOf("key" to listOf("value")),
                message = Buffer().write(ByteArray(5_000)),
                methodSpec = MethodSpec(
                    path = "",
                    requestClass = Any::class,
                    responseClass = Any::class,
                    streamType = StreamType.UNARY,
                    idempotency = Idempotency.NO_SIDE_EFFECTS,
                ),
            ),
        )
        assertThat(request.url.encodedQuery).isEmpty()
        assertThat(request.httpMethod).isEqualTo(HTTPMethod.POST)
    }

    @Test
    fun getRequestCreatedForNoFallbackEnabled() {
        val config = ProtocolClientConfig(
            host = "https://connectrpc.com",
            serializationStrategy = serializationStrategy,
            compressionPools = emptyList(),
            getConfiguration = GETConfiguration.Enabled,
        )
        val connectInterceptor = ConnectInterceptor(config)
        val unaryFunction = connectInterceptor.unaryFunction()
        val request = unaryFunction.requestFunction(
            UnaryHTTPRequest(
                url = Url(config.host),
                contentType = "content_type",
                timeout = null,
                headers = mapOf("key" to listOf("value")),
                message = Buffer().write(ByteArray(5_000)),
                methodSpec = MethodSpec(
                    path = "",
                    requestClass = Any::class,
                    responseClass = Any::class,
                    streamType = StreamType.UNARY,
                    idempotency = Idempotency.NO_SIDE_EFFECTS,
                ),
            ),
        )
        val queryMap = parseQuery(request)
        assertThat(queryMap.get(GETConstants.MESSAGE_QUERY_PARAM_KEY)).isNotNull()
        assertThat(queryMap.get(GETConstants.BASE64_QUERY_PARAM_KEY)).isEqualTo("1")
        assertThat(queryMap.get(GETConstants.ENCODING_QUERY_PARAM_KEY)).isEqualTo("encoding_name")
        assertThat(queryMap.get(GETConstants.CONNECT_VERSION_QUERY_PARAM_KEY)).isEqualTo("v1")
        assertThat(request.httpMethod).isEqualTo(HTTPMethod.GET)
    }

    @Test
    fun unaryPostRequestCreatedWithGetDisabled() {
        val config = ProtocolClientConfig(
            host = "https://connectrpc.com",
            serializationStrategy = serializationStrategy,
            compressionPools = emptyList(),
            getConfiguration = GETConfiguration.Disabled,
        )
        val connectInterceptor = ConnectInterceptor(config)
        val unaryFunction = connectInterceptor.unaryFunction()

        val request = unaryFunction.requestFunction(
            UnaryHTTPRequest(
                url = Url(config.host),
                contentType = "content_type",
                timeout = null,
                headers = mapOf("key" to listOf("value")),
                message = Buffer().write(ByteArray(5_000)),
                methodSpec = MethodSpec(
                    path = "",
                    requestClass = Any::class,
                    responseClass = Any::class,
                    streamType = StreamType.UNARY,
                    idempotency = Idempotency.NO_SIDE_EFFECTS,
                ),
            ),
        )
        assertThat(request.url.encodedQuery).isEmpty()
        assertThat(request.httpMethod).isEqualTo(HTTPMethod.POST)
    }

    @Test
    fun getRequestPreservesPathPrefix() {
        val config = ProtocolClientConfig(
            host = "https://connectrpc.com/api/v1",
            serializationStrategy = serializationStrategy,
            compressionPools = emptyList(),
            getConfiguration = GETConfiguration.Enabled,
        )
        val connectInterceptor = ConnectInterceptor(config)
        val unaryFunction = connectInterceptor.unaryFunction()

        // In actual usage, ProtocolClient.urlFromMethodSpec already adds methodSpec.path to the URL
        // before passing to the interceptor. This test simulates that behavior.
        val request = unaryFunction.requestFunction(
            UnaryHTTPRequest(
                url = Url("https://connectrpc.com/api/v1/service/Method"),
                contentType = "content_type",
                timeout = null,
                headers = emptyMap(),
                message = Buffer(),
                methodSpec = MethodSpec(
                    path = "service/Method",
                    requestClass = Any::class,
                    responseClass = Any::class,
                    streamType = StreamType.UNARY,
                    idempotency = Idempotency.NO_SIDE_EFFECTS,
                ),
            ),
        )

        assertThat(request.url.encodedPath).isEqualTo("/api/v1/service/Method")
        assertThat(request.httpMethod).isEqualTo(HTTPMethod.GET)
    }

    private fun parseQuery(request: HTTPRequest) = request.url.encodedQuery
        .split("&")
        .map { str ->
            val split = str.split("=")
            split[0] to split[1]
        }
        .foldRight(mutableMapOf<String, String>()) { pair, acc ->
            acc.put(pair.first, pair.second)
            acc
        }
}

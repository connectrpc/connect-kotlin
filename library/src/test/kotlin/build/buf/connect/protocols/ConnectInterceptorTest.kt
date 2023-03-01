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
import build.buf.connect.ErrorDetailParser
import build.buf.connect.ProtocolClientConfig
import build.buf.connect.SerializationStrategy
import build.buf.connect.StreamResult
import build.buf.connect.compression.GzipCompressionPool
import build.buf.connect.compression.RequestCompression
import build.buf.connect.http.HTTPRequest
import build.buf.connect.http.HTTPResponse
import build.buf.connect.http.TracingInfo
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okio.Buffer
import okio.ByteString.Companion.encodeUtf8
import okio.internal.commonAsUtf8ToByteArray
import okio.internal.commonToUtf8String
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.net.URL

class ConnectInterceptorTest {
    private val errorDetailParser: ErrorDetailParser = mock { }
    private val serializationStrategy: SerializationStrategy = mock { }

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    @Before
    fun setup() {
        whenever(serializationStrategy.errorDetailParser()).thenReturn(errorDetailParser)
        whenever(serializationStrategy.serializationName()).thenReturn("encoding_type")
    }

    /*
     * Unary
     */
    @Test
    fun requestHeaders() {
        val config = ProtocolClientConfig(
            host = "https://buf.build",
            serializationStrategy = serializationStrategy,
            compressionPools = emptyList()
        )
        val connectInterceptor = ConnectInterceptor(config)
        val unaryFunction = connectInterceptor.unaryFunction()

        val request = unaryFunction.requestFunction(
            HTTPRequest(
                url = URL(config.host),
                contentType = "content_type",
                headers = mapOf("key" to listOf("value"))
            )
        )
        assertThat(request.headers[CONNECT_PROTOCOL_VERSION_KEY]).containsExactly(CONNECT_PROTOCOL_VERSION_VALUE)
        assertThat(request.headers[ACCEPT_ENCODING]).isNullOrEmpty()
        assertThat(request.headers[CONTENT_ENCODING]).isNullOrEmpty()
        assertThat(request.headers["key"]).containsExactly("value")
        assertThat(request.contentType).isEqualTo("content_type")
    }

    @Test
    fun uncompressedRequestMessage() {
        val config = ProtocolClientConfig(
            host = "https://buf.build",
            serializationStrategy = serializationStrategy
        )
        val connectInterceptor = ConnectInterceptor(config)
        val unaryFunction = connectInterceptor.unaryFunction()

        val request = unaryFunction.requestFunction(
            HTTPRequest(
                url = URL(config.host),
                contentType = "content_type",
                headers = emptyMap(),
                message = "message".commonAsUtf8ToByteArray()
            )
        )
        assertThat(request.message!!.commonToUtf8String()).isEqualTo("message")
    }

    @Test
    fun compressedRequestMessage() {
        val config = ProtocolClientConfig(
            host = "https://buf.build",
            serializationStrategy = serializationStrategy,
            requestCompression = RequestCompression(1, GzipCompressionPool),
            compressionPools = listOf(GzipCompressionPool)
        )
        val connectInterceptor = ConnectInterceptor(config)
        val unaryFunction = connectInterceptor.unaryFunction()

        val request = unaryFunction.requestFunction(
            HTTPRequest(
                url = URL(config.host),
                contentType = "content_type",
                headers = emptyMap(),
                message = "message".commonAsUtf8ToByteArray()
            )
        )
        val decompressed = GzipCompressionPool.decompress(Buffer().write(request.message!!))
        assertThat(decompressed.readUtf8()).isEqualTo("message")
    }

    @Test
    fun uncompressedResponseMessage() {
        val config = ProtocolClientConfig(
            host = "https://buf.build",
            serializationStrategy = serializationStrategy,
            compressionPools = listOf(GzipCompressionPool)
        )
        val connectInterceptor = ConnectInterceptor(config)
        val unaryFunction = connectInterceptor.unaryFunction()

        val response = unaryFunction.responseFunction(
            HTTPResponse(
                code = Code.OK,
                headers = emptyMap(),
                message = Buffer().write("message".encodeUtf8()),
                trailers = emptyMap(),
                tracingInfo = null
            )
        )
        assertThat(response.message.readUtf8()).isEqualTo("message")
    }

    @Test
    fun compressedResponseMessage() {
        val config = ProtocolClientConfig(
            host = "https://buf.build",
            serializationStrategy = serializationStrategy,
            compressionPools = listOf(GzipCompressionPool)
        )
        val connectInterceptor = ConnectInterceptor(config)
        val unaryFunction = connectInterceptor.unaryFunction()

        val response = unaryFunction.responseFunction(
            HTTPResponse(
                code = Code.OK,
                headers = mapOf(CONTENT_ENCODING to listOf(GzipCompressionPool.name())),
                message = GzipCompressionPool.compress(Buffer().write("message".encodeUtf8())),
                trailers = emptyMap(),
                tracingInfo = null
            )
        )
        assertThat(response.message.readUtf8()).isEqualTo("message")
    }

    @Test
    fun responseError() {
        val config = ProtocolClientConfig(
            host = "https://buf.build",
            serializationStrategy = serializationStrategy,
            compressionPools = listOf(GzipCompressionPool)
        )
        val connectInterceptor = ConnectInterceptor(config)
        val unaryFunction = connectInterceptor.unaryFunction()
        val error = ErrorPayloadJSON(
            "resource_exhausted",
            "no more resources!",
            listOf(
                ErrorDetailPayloadJSON(
                    "type",
                    "value"
                )
            )
        )
        val adapter = moshi.adapter(ErrorPayloadJSON::class.java)
        val json = adapter.toJson(error)

        val response = unaryFunction.responseFunction(
            HTTPResponse(
                code = Code.UNAVAILABLE,
                message = Buffer().write(json.encodeUtf8()),
                headers = emptyMap(),
                trailers = emptyMap(),
                tracingInfo = null
            )
        )
        assertThat(response.error!!.code).isEqualTo(Code.RESOURCE_EXHAUSTED)
        assertThat(response.error!!.message).isEqualTo("no more resources!")
        val connectErrorDetail = response.error!!.details.singleOrNull()!!
        assertThat(connectErrorDetail.type).isEqualTo("type")
        assertThat(connectErrorDetail.payload).isEqualTo("value".encodeUtf8())
    }

    @Test
    fun responseErrorBadJSON() {
        val config = ProtocolClientConfig(
            host = "https://buf.build",
            serializationStrategy = serializationStrategy,
            compressionPools = listOf(GzipCompressionPool)
        )
        val connectInterceptor = ConnectInterceptor(config)
        val unaryFunction = connectInterceptor.unaryFunction()

        val response = unaryFunction.responseFunction(
            HTTPResponse(
                code = Code.UNAVAILABLE,
                message = Buffer().write("garbage json".encodeUtf8()),
                headers = emptyMap(),
                trailers = emptyMap(),
                tracingInfo = null
            )
        )
        assertThat(response.error!!.code).isEqualTo(Code.UNAVAILABLE)
    }

    @Test
    fun tracingInfoForwardedOnUnaryResponse() {
        val config = ProtocolClientConfig(
            host = "https://buf.build",
            serializationStrategy = serializationStrategy
        )
        val grpcWebInterceptor = ConnectInterceptor(config)
        val unaryFunction = grpcWebInterceptor.unaryFunction()

        val result = unaryFunction.responseFunction(
            HTTPResponse(
                Code.UNKNOWN,
                emptyMap(),
                Buffer(),
                emptyMap(),
                TracingInfo(888)
            )
        )
        assertThat(result.tracingInfo!!.httpStatus).isEqualTo(888)
    }

    /*
     * Streaming
     */
    @Test
    fun streamingRequestHeadersWithCompression() {
        val config = ProtocolClientConfig(
            host = "https://buf.build",
            serializationStrategy = serializationStrategy,
            requestCompression = RequestCompression(1000, GzipCompressionPool),
            compressionPools = listOf(GzipCompressionPool)
        )
        val connectInterceptor = ConnectInterceptor(config)
        val streamFunction = connectInterceptor.streamFunction()

        val request = streamFunction.requestFunction(
            HTTPRequest(
                url = URL(config.host),
                contentType = "content_type",
                headers = mapOf("key" to listOf("value"))
            )
        )
        assertThat(request.headers[CONNECT_PROTOCOL_VERSION_KEY]).containsExactly(CONNECT_PROTOCOL_VERSION_VALUE)
        assertThat(request.headers[CONNECT_STREAMING_ACCEPT_ENCODING]).containsExactly(GzipCompressionPool.name())
        assertThat(request.headers[CONNECT_STREAMING_CONTENT_ENCODING]).containsExactly(GzipCompressionPool.name())
        assertThat(request.headers["key"]).containsExactly("value")
        assertThat(request.contentType).isEqualTo("content_type")
    }

    @Test
    fun streamingRequestHeadersNoCompression() {
        val config = ProtocolClientConfig(
            host = "https://buf.build",
            serializationStrategy = serializationStrategy,
            compressionPools = emptyList()
        )
        val connectInterceptor = ConnectInterceptor(config)
        val streamFunction = connectInterceptor.streamFunction()

        val request = streamFunction.requestFunction(
            HTTPRequest(
                url = URL(config.host),
                contentType = "content_type",
                headers = mapOf("key" to listOf("value"))
            )
        )
        assertThat(request.headers[CONNECT_PROTOCOL_VERSION_KEY]).containsExactly(CONNECT_PROTOCOL_VERSION_VALUE)
        assertThat(request.headers[CONNECT_STREAMING_ACCEPT_ENCODING]).isNullOrEmpty()
        assertThat(request.headers[CONNECT_STREAMING_CONTENT_ENCODING]).isNullOrEmpty()
        assertThat(request.headers["key"]).containsExactly("value")
        assertThat(request.contentType).isEqualTo("content_type")
    }

    @Test
    fun uncompressedStreamingRequestMessage() {
        val config = ProtocolClientConfig(
            host = "https://buf.build",
            serializationStrategy = serializationStrategy
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
            host = "https://buf.build",
            serializationStrategy = serializationStrategy,
            compressionPools = listOf(GzipCompressionPool)
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
            host = "https://buf.build",
            serializationStrategy = serializationStrategy,
            compressionPools = listOf(GzipCompressionPool)
        )
        val connectInterceptor = ConnectInterceptor(config)
        val streamFunction = connectInterceptor.streamFunction()

        val result = streamFunction.streamResultFunction(
            StreamResult.Headers(
                headers = mapOf(
                    "trailer-x-some-key" to listOf("some_value"),
                    CONNECT_STREAMING_CONTENT_ENCODING to listOf("gzip")
                )
            )
        )

        assertThat(result).isOfAnyClassIn(StreamResult.Headers::class.java)
        val headerResult = result as StreamResult.Headers
        assertThat(headerResult.headers).isEqualTo(mapOf(CONNECT_STREAMING_CONTENT_ENCODING to listOf("gzip")))
    }

    @Test
    fun uncompressedStreamingResponseMessage() {
        val config = ProtocolClientConfig(
            host = "https://buf.build",
            serializationStrategy = serializationStrategy,
            compressionPools = listOf(GzipCompressionPool)
        )
        val connectInterceptor = ConnectInterceptor(config)
        val streamFunction = connectInterceptor.streamFunction()
        // Send headers for no compression.
        streamFunction.streamResultFunction(StreamResult.Headers(emptyMap()))

        val envelopedMessage = Envelope.pack(Buffer().write("hello".encodeUtf8()))
        val result = streamFunction.streamResultFunction(
            StreamResult.Message(
                Buffer().write(envelopedMessage.readByteString())
            )
        )

        assertThat(result).isOfAnyClassIn(StreamResult.Message::class.java)
        val streamMessage = result as StreamResult.Message
        assertThat(streamMessage.message.readUtf8()).isEqualTo("hello")
    }

    @Test
    fun compressedStreamingResponseMessage() {
        val config = ProtocolClientConfig(
            host = "https://buf.build",
            serializationStrategy = serializationStrategy,
            requestCompression = RequestCompression(1, GzipCompressionPool),
            compressionPools = listOf(GzipCompressionPool)
        )
        val connectInterceptor = ConnectInterceptor(config)
        val streamFunction = connectInterceptor.streamFunction()
        // Send headers for gzip compression.
        streamFunction.streamResultFunction(
            StreamResult.Headers(
                headers = mapOf(
                    CONNECT_STREAMING_CONTENT_ENCODING to listOf("gzip")
                )
            )
        )

        val envelopedMessage = Envelope.pack(Buffer().write("hello".encodeUtf8()), GzipCompressionPool, 1)
        val result = streamFunction.streamResultFunction(
            StreamResult.Message(
                Buffer().write(envelopedMessage.readByteString())
            )
        )

        assertThat(result).isOfAnyClassIn(StreamResult.Message::class.java)
        val streamMessage = result as StreamResult.Message
        assertThat(streamMessage.message.readUtf8()).isEqualTo("hello")
    }

    @Test
    fun endStreamOnResponseMessage() {
        val config = ProtocolClientConfig(
            host = "https://buf.build",
            serializationStrategy = serializationStrategy
        )
        val connectInterceptor = ConnectInterceptor(config)
        val streamFunction = connectInterceptor.streamFunction()
        val error = ErrorPayloadJSON(
            "resource_exhausted",
            "no more resources!",
            listOf(
                ErrorDetailPayloadJSON(
                    "type",
                    "value"
                )
            )
        )
        val endStream = EndStreamResponseJSON(
            error = error,
            metadata = emptyMap()
        )
        val adapter = moshi.adapter(EndStreamResponseJSON::class.java)
        val json = adapter.toJson(endStream)
        val endStreamMessage = Buffer()
        val endStreamHeaderByte = 1
        endStreamMessage.writeByte(endStreamHeaderByte.shl(1))
        endStreamMessage.writeInt(json.length)
        endStreamMessage.write(json.encodeUtf8())

        val result = streamFunction.streamResultFunction(
            StreamResult.Message(
                Buffer().write(endStreamMessage.readByteString())
            )
        )
        assertThat(result).isOfAnyClassIn(StreamResult.Complete::class.java)
        val completion = result as StreamResult.Complete
        assertThat(completion.code).isEqualTo(Code.RESOURCE_EXHAUSTED)
        val connectError = completion.connectError()!!
        assertThat(connectError.message).isEqualTo("no more resources!")
        val errorDetail = connectError.details.single()
        assertThat(errorDetail.type).isEqualTo("type")
        assertThat(errorDetail.payload).isEqualTo("value".encodeUtf8())
    }

    @Test
    fun endStreamResponseBadJSON() {
        val config = ProtocolClientConfig(
            host = "https://buf.build",
            serializationStrategy = serializationStrategy
        )
        val connectInterceptor = ConnectInterceptor(config)
        val streamFunction = connectInterceptor.streamFunction()

        val result = streamFunction.streamResultFunction(
            StreamResult.Message(
                Buffer().write("some garbage json".encodeUtf8())
            )
        )
        assertThat(result).isOfAnyClassIn(StreamResult.Complete::class.java)
        val completion = result as StreamResult.Complete
        assertThat(completion.code).isEqualTo(Code.UNKNOWN)
    }

    @Test
    fun endStreamOnTrailers() {
        val config = ProtocolClientConfig(
            host = "https://buf.build",
            serializationStrategy = serializationStrategy
        )
        val connectInterceptor = ConnectInterceptor(config)
        val streamFunction = connectInterceptor.streamFunction()

        val result = streamFunction.streamResultFunction(
            StreamResult.Complete(
                code = Code.OK,
                trailers = mapOf(
                    "key" to listOf("value")
                )
            )
        )

        assertThat(result).isOfAnyClassIn(StreamResult.Complete::class.java)
        val completion = result as StreamResult.Complete
        assertThat(completion.code).isEqualTo(Code.OK)
        assertThat(completion.trailers["key"]).containsExactly("value")
    }

    @Test
    fun endStreamForwardsErrors() {
        val config = ProtocolClientConfig(
            host = "https://buf.build",
            serializationStrategy = serializationStrategy
        )
        val connectInterceptor = ConnectInterceptor(config)
        val streamFunction = connectInterceptor.streamFunction()

        val result = streamFunction.streamResultFunction(
            StreamResult.Complete(
                code = Code.UNKNOWN,
                error = ConnectError(
                    Code.UNKNOWN,
                    message = "error_message"
                )
            )
        )

        assertThat(result).isOfAnyClassIn(StreamResult.Complete::class.java)
        val completion = result as StreamResult.Complete
        assertThat(completion.code).isEqualTo(Code.UNKNOWN)
    }
}

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
import build.buf.connect.MethodSpec
import build.buf.connect.ProtocolClientConfig
import build.buf.connect.RequestCompression
import build.buf.connect.SerializationStrategy
import build.buf.connect.StreamResult
import build.buf.connect.compression.GzipCompressionPool
import build.buf.connect.http.HTTPRequest
import build.buf.connect.http.HTTPResponse
import build.buf.connect.http.TracingInfo
import okio.Buffer
import okio.ByteString.Companion.encodeUtf8
import okio.internal.commonAsUtf8ToByteArray
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.net.URL

class GRPCWebInterceptorTest {

    private val errorDetailParser: ErrorDetailParser = mock { }
    private val serializationStrategy: SerializationStrategy = mock { }

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
            serializationStrategy = serializationStrategy
        )
        val grpcWebInterceptor = GRPCWebInterceptor(config)
        val unaryFunction = grpcWebInterceptor.unaryFunction()

        val request = unaryFunction.requestFunction(
            HTTPRequest(
                url = URL(config.host),
                contentType = "",
                headers = mapOf("key" to listOf("value")),
                methodSpec = MethodSpec(
                    path = "",
                    requestClass = Any::class,
                    responseClass = Any::class
                )
            )
        )
        assertThat(request.headers[ACCEPT_ENCODING]).isNullOrEmpty()
        assertThat(request.headers[CONTENT_ENCODING]).isNullOrEmpty()
        assertThat(request.headers["key"]).containsExactly("value")
        assertThat(request.contentType).isEqualTo("application/grpc-web+${serializationStrategy.serializationName()}")
    }

    @Test
    fun uncompressedRequestMessage() {
        val config = ProtocolClientConfig(
            host = "https://buf.build",
            serializationStrategy = serializationStrategy
        )
        val grpcWebInterceptor = GRPCWebInterceptor(config)
        val unaryFunction = grpcWebInterceptor.unaryFunction()

        val request = unaryFunction.requestFunction(
            HTTPRequest(
                url = URL(config.host),
                contentType = "",
                headers = emptyMap(),
                message = "message".commonAsUtf8ToByteArray(),
                methodSpec = MethodSpec(
                    path = "",
                    requestClass = Any::class,
                    responseClass = Any::class
                )
            )
        )
        val (_, message) = Envelope.unpackWithHeaderByte(Buffer().write(request.message!!))
        assertThat(message.readUtf8()).isEqualTo("message")
    }

    @Test
    fun compressedRequestMessage() {
        val config = ProtocolClientConfig(
            host = "https://buf.build",
            serializationStrategy = serializationStrategy,
            requestCompression = RequestCompression(1, GzipCompressionPool),
            compressionPools = listOf(GzipCompressionPool)
        )
        val grpcWebInterceptor = GRPCWebInterceptor(config)
        val unaryFunction = grpcWebInterceptor.unaryFunction()

        val request = unaryFunction.requestFunction(
            HTTPRequest(
                url = URL(config.host),
                contentType = "",
                headers = emptyMap(),
                message = "message".commonAsUtf8ToByteArray(),
                methodSpec = MethodSpec(
                    path = "",
                    requestClass = Any::class,
                    responseClass = Any::class
                )
            )
        )
        val (_, decompressed) = Envelope.unpackWithHeaderByte(Buffer().write(request.message!!), GzipCompressionPool)
        assertThat(decompressed.readUtf8()).isEqualTo("message")
    }

    @Test
    fun uncompressedResponseMessage() {
        val config = ProtocolClientConfig(
            host = "https://buf.build",
            serializationStrategy = serializationStrategy,
            compressionPools = listOf(GzipCompressionPool)
        )
        val grpcWebInterceptor = GRPCWebInterceptor(config)
        val unaryFunction = grpcWebInterceptor.unaryFunction()

        val envelopedMessage = Envelope.pack(Buffer().write("message".encodeUtf8()))
        val response = unaryFunction.responseFunction(
            HTTPResponse(
                code = Code.OK,
                headers = emptyMap(),
                message = envelopedMessage,
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
        val grpcWebInterceptor = GRPCWebInterceptor(config)
        val unaryFunction = grpcWebInterceptor.unaryFunction()

        val envelopedMessage = Envelope.pack(Buffer().write("message".encodeUtf8()), GzipCompressionPool, 1)
        val response = unaryFunction.responseFunction(
            HTTPResponse(
                code = Code.OK,
                headers = mapOf(GRPC_ENCODING to listOf(GzipCompressionPool.name())),
                message = envelopedMessage,
                trailers = emptyMap(),
                tracingInfo = null
            )
        )
        assertThat(response.message.readUtf8()).isEqualTo("message")
    }

    @Test
    fun failureOnResponseHeaders() {
        val config = ProtocolClientConfig(
            host = "https://buf.build",
            serializationStrategy = serializationStrategy,
            compressionPools = listOf(GzipCompressionPool)
        )
        val grpcWebInterceptor = GRPCWebInterceptor(config)
        val unaryFunction = grpcWebInterceptor.unaryFunction()

        val response = unaryFunction.responseFunction(
            HTTPResponse(
                code = Code.OK,
                headers = mapOf(
                    GRPC_STATUS_TRAILER to listOf("${Code.RESOURCE_EXHAUSTED.value}")
                ),
                message = Buffer(),
                trailers = emptyMap(),
                tracingInfo = null
            )
        )
        assertThat(response.error!!.code).isEqualTo(Code.RESOURCE_EXHAUSTED)
    }

    @Test
    fun responseErrorWithOnlyTrailers() {
        val config = ProtocolClientConfig(
            host = "https://buf.build",
            serializationStrategy = serializationStrategy,
            compressionPools = listOf(GzipCompressionPool)
        )
        val grpcWebInterceptor = GRPCWebInterceptor(config)
        val unaryFunction = grpcWebInterceptor.unaryFunction()

        val trailersPayload = "$GRPC_STATUS_TRAILER:${Code.RESOURCE_EXHAUSTED.value}".encodeUtf8()
        val trailers = Buffer()
            .writeByte(TRAILERS_BIT)
            .writeInt(trailersPayload.size)
            .write(trailersPayload)
        val response = unaryFunction.responseFunction(
            HTTPResponse(
                code = Code.OK,
                message = trailers,
                headers = emptyMap(),
                trailers = emptyMap(),
                tracingInfo = null
            )
        )
        assertThat(response.code).isEqualTo(Code.RESOURCE_EXHAUSTED)
    }

    @Test
    fun responseErrorWithTrailers() {
        val config = ProtocolClientConfig(
            host = "https://buf.build",
            serializationStrategy = serializationStrategy,
            compressionPools = listOf(GzipCompressionPool)
        )
        val grpcWebInterceptor = GRPCWebInterceptor(config)
        val unaryFunction = grpcWebInterceptor.unaryFunction()

        // Encode message + encode trailers into the same message block.
        val message = Envelope.pack(Buffer().write("message".encodeUtf8()))
        val trailersPayload = "$GRPC_STATUS_TRAILER:${Code.RESOURCE_EXHAUSTED.value}".encodeUtf8()
        val trailers = Buffer()
            .writeByte(TRAILERS_BIT)
            .writeInt(trailersPayload.size)
            .write(trailersPayload)
        val responseBody = Buffer()
            .write(message.readByteArray())
            .write(trailers.readByteArray())
        val response = unaryFunction.responseFunction(
            HTTPResponse(
                code = Code.OK,
                message = responseBody,
                headers = emptyMap(),
                trailers = emptyMap(),
                tracingInfo = null
            )
        )
        assertThat(response.error!!.code).isEqualTo(Code.RESOURCE_EXHAUSTED)
    }

    @Test
    fun tracingInfoForwardedOnUnaryResponse() {
        val config = ProtocolClientConfig(
            host = "https://buf.build",
            serializationStrategy = serializationStrategy
        )
        val grpcWebInterceptor = GRPCWebInterceptor(config)
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
        val grpcWebInterceptor = GRPCWebInterceptor(config)
        val streamFunction = grpcWebInterceptor.streamFunction()

        val request = streamFunction.requestFunction(
            HTTPRequest(
                url = URL(config.host),
                contentType = "content_type",
                headers = mapOf("key" to listOf("value")),
                methodSpec = MethodSpec(
                    path = "",
                    requestClass = Any::class,
                    responseClass = Any::class
                )
            )
        )
        assertThat(request.contentType).isEqualTo("application/grpc-web+${serializationStrategy.serializationName()}")
        assertThat(request.headers[GRPC_USER_AGENT]).containsExactly("@bufbuild/connect-kotlin dev")
        assertThat(request.headers[GRPC_TE_HEADER]).containsExactly("trailers")
        assertThat(request.headers[GRPC_ENCODING]).containsExactly(GzipCompressionPool.name())
        assertThat(request.headers["key"]).containsExactly("value")
    }

    @Test
    fun streamingRequestHeadersNoCompression() {
        val config = ProtocolClientConfig(
            host = "https://buf.build",
            serializationStrategy = serializationStrategy
        )
        val grpcWebInterceptor = GRPCWebInterceptor(config)
        val streamFunction = grpcWebInterceptor.streamFunction()

        val request = streamFunction.requestFunction(
            HTTPRequest(
                url = URL(config.host),
                contentType = "content_type",
                headers = mapOf("key" to listOf("value")),
                methodSpec = MethodSpec(
                    path = "",
                    requestClass = Any::class,
                    responseClass = Any::class
                )
            )
        )
        assertThat(request.headers[GRPC_ENCODING]).isNullOrEmpty()
        assertThat(request.headers["key"]).containsExactly("value")
    }

    @Test
    fun uncompressedStreamingRequestMessage() {
        val config = ProtocolClientConfig(
            host = "https://buf.build",
            serializationStrategy = serializationStrategy
        )
        val grpcWebInterceptor = GRPCWebInterceptor(config)
        val streamFunction = grpcWebInterceptor.streamFunction()

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
        val grpcWebInterceptor = GRPCWebInterceptor(config)
        val streamFunction = grpcWebInterceptor.streamFunction()

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
        val grpcWebInterceptor = GRPCWebInterceptor(config)
        val streamFunction = grpcWebInterceptor.streamFunction()

        val result = streamFunction.streamResultFunction(
            StreamResult.Headers(
                headers = mapOf(
                    // Doesn't get passed as headers.
                    "trailer-x-some-key" to listOf("some_value"),
                    GRPC_ENCODING to listOf("gzip")
                )
            )
        )

        assertThat(result).isOfAnyClassIn(StreamResult.Headers::class.java)
        val headerResult = result as StreamResult.Headers
        assertThat(headerResult.headers[GRPC_ENCODING]).containsExactly("gzip")
        assertThat(headerResult.headers.containsKey("trailer-x-some-key")).isFalse()
    }

    @Test
    fun uncompressedStreamingResponseMessage() {
        val config = ProtocolClientConfig(
            host = "https://buf.build",
            serializationStrategy = serializationStrategy,
            compressionPools = listOf(GzipCompressionPool)
        )
        val grpcWebInterceptor = GRPCWebInterceptor(config)
        val streamFunction = grpcWebInterceptor.streamFunction()
        // Send headers for no compression.
        streamFunction.streamResultFunction(StreamResult.Headers(emptyMap()))

        val envelopedMessage = Envelope.pack(Buffer().write("hello".encodeUtf8()))
        val result = streamFunction.streamResultFunction(
            StreamResult.Message(envelopedMessage)
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
            compressionPools = listOf(GzipCompressionPool)
        )
        val grpcWebInterceptor = GRPCWebInterceptor(config)
        val streamFunction = grpcWebInterceptor.streamFunction()
        // Send headers for gzip compression.
        streamFunction.streamResultFunction(
            StreamResult.Headers(
                headers = mapOf(
                    GRPC_ENCODING to listOf("gzip")
                )
            )
        )

        val envelopedMessage = Envelope.pack(Buffer().write("hello".encodeUtf8()), GzipCompressionPool, 1)
        val result = streamFunction.streamResultFunction(
            StreamResult.Message(envelopedMessage)
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
        val grpcWebInterceptor = GRPCWebInterceptor(config)
        val streamFunction = grpcWebInterceptor.streamFunction()
        val trailersPayload =
            "$GRPC_STATUS_TRAILER:${Code.RESOURCE_EXHAUSTED.value}\r\n$GRPC_MESSAGE_TRAILER:no more resources!".encodeUtf8()
        val trailers = Buffer()
            .writeByte(TRAILERS_BIT)
            .writeInt(trailersPayload.size)
            .write(trailersPayload)

        val result = streamFunction.streamResultFunction(
            StreamResult.Message(trailers)
        )
        assertThat(result).isOfAnyClassIn(StreamResult.Complete::class.java)
        val completion = result as StreamResult.Complete
        assertThat(completion.code).isEqualTo(Code.RESOURCE_EXHAUSTED)
        val connectError = completion.connectError()!!
        assertThat(connectError.message).isEqualTo("no more resources!")
    }

    @Test
    fun endStreamOnTrailers() {
        val config = ProtocolClientConfig(
            host = "https://buf.build",
            serializationStrategy = serializationStrategy
        )
        val grpcWebInterceptor = GRPCWebInterceptor(config)
        val streamFunction = grpcWebInterceptor.streamFunction()

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
        val grpcWebInterceptor = GRPCWebInterceptor(config)
        val streamFunction = grpcWebInterceptor.streamFunction()

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

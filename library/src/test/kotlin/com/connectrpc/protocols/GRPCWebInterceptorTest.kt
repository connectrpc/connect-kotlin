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
import com.connectrpc.ErrorDetailParser
import com.connectrpc.MethodSpec
import com.connectrpc.ProtocolClientConfig
import com.connectrpc.RequestCompression
import com.connectrpc.SerializationStrategy
import com.connectrpc.StreamResult
import com.connectrpc.StreamType
import com.connectrpc.compression.GzipCompressionPool
import com.connectrpc.http.HTTPRequest
import com.connectrpc.http.HTTPResponse
import com.connectrpc.http.UnaryHTTPRequest
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
            host = "https://connectrpc.com",
            serializationStrategy = serializationStrategy,
        )
        val grpcWebInterceptor = GRPCWebInterceptor(config)
        val unaryFunction = grpcWebInterceptor.unaryFunction()

        val request = unaryFunction.requestFunction(
            UnaryHTTPRequest(
                url = Url(config.host),
                contentType = "",
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
        assertThat(request.headers[ACCEPT_ENCODING]).isNullOrEmpty()
        assertThat(request.headers[CONTENT_ENCODING]).isNullOrEmpty()
        assertThat(request.headers[GRPC_TIMEOUT]).containsExactly("2500m")
        assertThat(request.headers["key"]).containsExactly("value")
        assertThat(request.contentType).isEqualTo("application/grpc-web+${serializationStrategy.serializationName()}")
        assertThat(request.headers[GRPC_WEB_USER_AGENT]).containsExactly("grpc-kotlin-connect/dev")
    }

    @Test
    fun requestHeadersCustomUserAgent() {
        val config = ProtocolClientConfig(
            host = "https://buf.build",
            serializationStrategy = serializationStrategy,
        )
        val grpcWebInterceptor = GRPCWebInterceptor(config)
        val unaryFunction = grpcWebInterceptor.unaryFunction()

        val request = unaryFunction.requestFunction(
            UnaryHTTPRequest(
                url = Url(config.host),
                contentType = "",
                timeout = null,
                headers = mapOf("X-User-Agent" to listOf("custom-user-agent")),
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
        assertThat(request.headers[GRPC_WEB_USER_AGENT]).isNull()
        assertThat(request.headers["X-User-Agent"]).containsExactly("custom-user-agent")
    }

    @Test
    fun uncompressedRequestMessage() {
        val config = ProtocolClientConfig(
            host = "https://connectrpc.com",
            serializationStrategy = serializationStrategy,
        )
        val grpcWebInterceptor = GRPCWebInterceptor(config)
        val unaryFunction = grpcWebInterceptor.unaryFunction()

        val request = unaryFunction.requestFunction(
            UnaryHTTPRequest(
                url = Url(config.host),
                contentType = "",
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
        val (_, message) = Envelope.unpackWithHeaderByte(request.message)
        assertThat(message.readUtf8()).isEqualTo("message")
    }

    @Test
    fun compressedRequestMessage() {
        val config = ProtocolClientConfig(
            host = "https://connectrpc.com",
            serializationStrategy = serializationStrategy,
            requestCompression = RequestCompression(1, GzipCompressionPool),
            compressionPools = listOf(GzipCompressionPool),
        )
        val grpcWebInterceptor = GRPCWebInterceptor(config)
        val unaryFunction = grpcWebInterceptor.unaryFunction()

        val request = unaryFunction.requestFunction(
            UnaryHTTPRequest(
                url = Url(config.host),
                contentType = "",
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
        val (_, decompressed) = Envelope.unpackWithHeaderByte(request.message, GzipCompressionPool)
        assertThat(decompressed.readUtf8()).isEqualTo("message")
    }

    @Test
    fun uncompressedResponseMessage() {
        val config = ProtocolClientConfig(
            host = "https://connectrpc.com",
            serializationStrategy = serializationStrategy,
            compressionPools = listOf(GzipCompressionPool),
        )
        val grpcWebInterceptor = GRPCWebInterceptor(config)
        val unaryFunction = grpcWebInterceptor.unaryFunction()

        val responseBody = Envelope.pack(Buffer().write("message".encodeUtf8()), GzipCompressionPool, 0)
        // And add end-stream message w/ trailers, too
        val endStreamMessageContents = "grpc-status: 0\r\n".encodeUtf8()
        responseBody.writeByte(GRPCWebInterceptor.TRAILERS_BIT)
        responseBody.writeInt(endStreamMessageContents.size)
        responseBody.write(endStreamMessageContents)
        val response = unaryFunction.responseFunction(
            HTTPResponse(
                status = 200,
                headers = mapOf(
                    CONTENT_TYPE to listOf("application/grpc-web+encoding_type"),
                    GRPC_ENCODING to listOf("gzip"),
                ),
                message = responseBody,
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
        val grpcWebInterceptor = GRPCWebInterceptor(config)
        val unaryFunction = grpcWebInterceptor.unaryFunction()

        val responseBody = Envelope.pack(Buffer().write("message".encodeUtf8()), GzipCompressionPool, 1)
        // And add end-stream message w/ trailers, too
        val endStreamMessageContents = "grpc-status: 0\r\n".encodeUtf8()
        responseBody.writeByte(GRPCWebInterceptor.TRAILERS_BIT)
        responseBody.writeInt(endStreamMessageContents.size)
        responseBody.write(endStreamMessageContents)
        val response = unaryFunction.responseFunction(
            HTTPResponse(
                status = 200,
                headers = mapOf(
                    CONTENT_TYPE to listOf("application/grpc-web+encoding_type"),
                    GRPC_ENCODING to listOf(GzipCompressionPool.name()),
                ),
                message = responseBody,
                trailers = emptyMap(),
            ),
        )
        assertThat(response.message.readUtf8()).isEqualTo("message")
    }

    @Test
    fun failureOnResponseHeaders() {
        val config = ProtocolClientConfig(
            host = "https://connectrpc.com",
            serializationStrategy = serializationStrategy,
            compressionPools = listOf(GzipCompressionPool),
        )
        val grpcWebInterceptor = GRPCWebInterceptor(config)
        val unaryFunction = grpcWebInterceptor.unaryFunction()

        val response = unaryFunction.responseFunction(
            HTTPResponse(
                status = 200,
                headers = mapOf(
                    CONTENT_TYPE to listOf("application/grpc-web+encoding_type"),
                    GRPC_STATUS_TRAILER to listOf("${Code.RESOURCE_EXHAUSTED.value}"),
                ),
                message = Buffer(),
                trailers = emptyMap(),
            ),
        )
        assertThat(response.cause!!.code).isEqualTo(Code.RESOURCE_EXHAUSTED)
    }

    @Test
    fun responseErrorWithOnlyTrailers() {
        val config = ProtocolClientConfig(
            host = "https://connectrpc.com",
            serializationStrategy = serializationStrategy,
            compressionPools = listOf(GzipCompressionPool),
        )
        val grpcWebInterceptor = GRPCWebInterceptor(config)
        val unaryFunction = grpcWebInterceptor.unaryFunction()

        val trailersPayload = "$GRPC_STATUS_TRAILER:${Code.RESOURCE_EXHAUSTED.value}".encodeUtf8()
        val trailers = Buffer()
            .writeByte(GRPCWebInterceptor.TRAILERS_BIT)
            .writeInt(trailersPayload.size)
            .write(trailersPayload)
        val response = unaryFunction.responseFunction(
            HTTPResponse(
                status = 200,
                headers = mapOf(CONTENT_TYPE to listOf("application/grpc-web+encoding_type")),
                message = trailers,
                trailers = emptyMap(),
            ),
        )
        assertThat(response.cause!!.code).isEqualTo(Code.RESOURCE_EXHAUSTED)
    }

    @Test
    fun responseErrorWithTrailers() {
        val config = ProtocolClientConfig(
            host = "https://connectrpc.com",
            serializationStrategy = serializationStrategy,
            compressionPools = listOf(GzipCompressionPool),
        )
        val grpcWebInterceptor = GRPCWebInterceptor(config)
        val unaryFunction = grpcWebInterceptor.unaryFunction()

        // Encode message + encode trailers into the same message block.
        val message = Envelope.pack(Buffer().write("message".encodeUtf8()))
        val trailersPayload = "$GRPC_STATUS_TRAILER:${Code.RESOURCE_EXHAUSTED.value}".encodeUtf8()
        val trailers = Buffer()
            .writeByte(GRPCWebInterceptor.TRAILERS_BIT)
            .writeInt(trailersPayload.size)
            .write(trailersPayload)
        val responseBody = Buffer()
            .write(message.readByteArray())
            .write(trailers.readByteArray())
        val response = unaryFunction.responseFunction(
            HTTPResponse(
                status = 200,
                headers = mapOf(CONTENT_TYPE to listOf("application/grpc-web+encoding_type")),
                message = responseBody,
                trailers = emptyMap(),
            ),
        )
        assertThat(response.cause!!.code).isEqualTo(Code.RESOURCE_EXHAUSTED)
    }

    @Test
    fun tracingInfoForwardedOnUnaryResponse() {
        val config = ProtocolClientConfig(
            host = "https://connectrpc.com",
            serializationStrategy = serializationStrategy,
        )
        val grpcWebInterceptor = GRPCWebInterceptor(config)
        val unaryFunction = grpcWebInterceptor.unaryFunction()

        val result = unaryFunction.responseFunction(
            HTTPResponse(
                status = null,
                headers = emptyMap(),
                message = Buffer(),
                trailers = emptyMap(),
                cause = ConnectException(code = Code.UNKNOWN),
            ),
        )
        assertThat(result.cause!!.code).isEqualTo(Code.UNKNOWN)
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
        val grpcWebInterceptor = GRPCWebInterceptor(config)
        val streamFunction = grpcWebInterceptor.streamFunction()

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
        assertThat(request.contentType).isEqualTo("application/grpc-web+${serializationStrategy.serializationName()}")
        assertThat(request.headers[GRPC_WEB_USER_AGENT]).containsExactly("grpc-kotlin-connect/dev")
        assertThat(request.headers[GRPC_ENCODING]).containsExactly(GzipCompressionPool.name())
        assertThat(request.headers["key"]).containsExactly("value")
    }

    @Test
    fun streamingRequestHeadersCustomUserAgent() {
        val config = ProtocolClientConfig(
            host = "https://buf.build",
            serializationStrategy = serializationStrategy,
            requestCompression = RequestCompression(1000, GzipCompressionPool),
            compressionPools = listOf(GzipCompressionPool),
        )
        val grpcWebInterceptor = GRPCWebInterceptor(config)
        val streamFunction = grpcWebInterceptor.streamFunction()

        val request = streamFunction.requestFunction(
            HTTPRequest(
                url = Url(config.host),
                contentType = "content_type",
                timeout = null,
                headers = mapOf("X-User-Agent" to listOf("custom-user-agent")),
                methodSpec = MethodSpec(
                    path = "",
                    requestClass = Any::class,
                    responseClass = Any::class,
                    streamType = StreamType.BIDI,
                ),
            ),
        )
        // this will only work if we do a case-insensitive lookup of headers
        assertThat(request.headers[GRPC_WEB_USER_AGENT]).isNull()
        assertThat(request.headers["X-User-Agent"]).containsExactly("custom-user-agent")
    }

    @Test
    fun streamingRequestHeadersNoCompression() {
        val config = ProtocolClientConfig(
            host = "https://connectrpc.com",
            serializationStrategy = serializationStrategy,
        )
        val grpcWebInterceptor = GRPCWebInterceptor(config)
        val streamFunction = grpcWebInterceptor.streamFunction()

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
        assertThat(request.headers[GRPC_ENCODING]).isNullOrEmpty()
        assertThat(request.headers["key"]).containsExactly("value")
    }

    @Test
    fun uncompressedStreamingRequestMessage() {
        val config = ProtocolClientConfig(
            host = "https://connectrpc.com",
            serializationStrategy = serializationStrategy,
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
            host = "https://connectrpc.com",
            serializationStrategy = serializationStrategy,
            compressionPools = listOf(GzipCompressionPool),
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
            host = "https://connectrpc.com",
            serializationStrategy = serializationStrategy,
            compressionPools = listOf(GzipCompressionPool),
        )
        val grpcWebInterceptor = GRPCWebInterceptor(config)
        val streamFunction = grpcWebInterceptor.streamFunction()

        val result = streamFunction.streamResultFunction(
            StreamResult.Headers(
                headers = mapOf(
                    // Doesn't get passed as headers.
                    "trailer-x-some-key" to listOf("some_value"),
                    CONTENT_TYPE to listOf("application/grpc-web+encoding_type"),
                    GRPC_ENCODING to listOf("gzip"),
                ),
            ),
        )

        assertThat(result).isOfAnyClassIn(StreamResult.Headers::class.java)
        val headerResult = result as StreamResult.Headers
        assertThat(headerResult.headers[GRPC_ENCODING]).containsExactly("gzip")
        assertThat(headerResult.headers["trailer-x-some-key"]).containsExactly("some_value")
    }

    @Test
    fun uncompressedStreamingResponseMessage() {
        val config = ProtocolClientConfig(
            host = "https://connectrpc.com",
            serializationStrategy = serializationStrategy,
            compressionPools = listOf(GzipCompressionPool),
        )
        val grpcWebInterceptor = GRPCWebInterceptor(config)
        val streamFunction = grpcWebInterceptor.streamFunction()
        // Send headers for no compression.
        streamFunction.streamResultFunction(StreamResult.Headers(emptyMap()))

        val envelopedMessage = Envelope.pack(Buffer().write("hello".encodeUtf8()))
        val result = streamFunction.streamResultFunction(
            StreamResult.Message(envelopedMessage),
        )

        assertThat(result).isOfAnyClassIn(StreamResult.Message::class.java)
        val streamMessage = result as StreamResult.Message
        assertThat(streamMessage.message.readUtf8()).isEqualTo("hello")
    }

    @Test
    fun compressedStreamingResponseMessage() {
        val config = ProtocolClientConfig(
            host = "https://connectrpc.com",
            serializationStrategy = serializationStrategy,
            compressionPools = listOf(GzipCompressionPool),
        )
        val grpcWebInterceptor = GRPCWebInterceptor(config)
        val streamFunction = grpcWebInterceptor.streamFunction()
        // Send headers for gzip compression.
        streamFunction.streamResultFunction(
            StreamResult.Headers(
                headers = mapOf(
                    CONTENT_TYPE to listOf("application/grpc-web+encoding_type"),
                    GRPC_ENCODING to listOf("gzip"),
                ),
            ),
        )

        val envelopedMessage = Envelope.pack(Buffer().write("hello".encodeUtf8()), GzipCompressionPool, 1)
        val result = streamFunction.streamResultFunction(
            StreamResult.Message(envelopedMessage),
        )

        assertThat(result).isOfAnyClassIn(StreamResult.Message::class.java)
        val streamMessage = result as StreamResult.Message
        assertThat(streamMessage.message.readUtf8()).isEqualTo("hello")
    }

    @Test
    fun endStreamOnResponseMessage() {
        val config = ProtocolClientConfig(
            host = "https://connectrpc.com",
            serializationStrategy = serializationStrategy,
        )
        val grpcWebInterceptor = GRPCWebInterceptor(config)
        val streamFunction = grpcWebInterceptor.streamFunction()
        val trailersPayload =
            "$GRPC_STATUS_TRAILER:${Code.RESOURCE_EXHAUSTED.value}\r\n$GRPC_MESSAGE_TRAILER:no more resources!".encodeUtf8()
        val trailers = Buffer()
            .writeByte(GRPCWebInterceptor.TRAILERS_BIT)
            .writeInt(trailersPayload.size)
            .write(trailersPayload)

        val result = streamFunction.streamResultFunction(
            StreamResult.Message(trailers),
        )
        assertThat(result).isOfAnyClassIn(StreamResult.Complete::class.java)
        val completion = result as StreamResult.Complete
        assertThat(completion.cause!!.code).isEqualTo(Code.RESOURCE_EXHAUSTED)
        assertThat(completion.cause!!.message).isEqualTo("no more resources!")
    }

    @Test
    fun endStreamOnTrailers() {
        val config = ProtocolClientConfig(
            host = "https://connectrpc.com",
            serializationStrategy = serializationStrategy,
        )
        val grpcWebInterceptor = GRPCWebInterceptor(config)
        val streamFunction = grpcWebInterceptor.streamFunction()

        val result = streamFunction.streamResultFunction(
            StreamResult.Complete(
                trailers = mapOf(
                    "key" to listOf("value"),
                ),
            ),
        )

        assertThat(result).isOfAnyClassIn(StreamResult.Complete::class.java)
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
        val grpcWebInterceptor = GRPCWebInterceptor(config)
        val streamFunction = grpcWebInterceptor.streamFunction()

        val result = streamFunction.streamResultFunction(
            StreamResult.Complete(
                cause = ConnectException(
                    Code.UNKNOWN,
                    message = "error_message",
                ),
            ),
        )

        assertThat(result).isOfAnyClassIn(StreamResult.Complete::class.java)
        val completion = result as StreamResult.Complete
        assertThat(completion.cause!!.code).isEqualTo(Code.UNKNOWN)
    }
}

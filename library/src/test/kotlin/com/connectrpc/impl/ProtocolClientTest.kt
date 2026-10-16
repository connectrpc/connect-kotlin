// Copyright 2022-2026 The Connect Authors
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

package com.connectrpc.impl

import com.connectrpc.Code
import com.connectrpc.Codec
import com.connectrpc.ConnectException
import com.connectrpc.MethodSpec
import com.connectrpc.ProtocolClientConfig
import com.connectrpc.ResponseMessage
import com.connectrpc.SerializationStrategy
import com.connectrpc.StreamResult
import com.connectrpc.StreamType
import com.connectrpc.http.Cancelable
import com.connectrpc.http.HTTPClientInterface
import com.connectrpc.http.HTTPRequest
import com.connectrpc.http.HTTPResponse
import com.connectrpc.http.Stream
import com.connectrpc.http.Timeout
import com.connectrpc.http.UnaryHTTPRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okio.Buffer
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds

class ProtocolClientTest {
    private val serializationStrategy: SerializationStrategy = mock { }
    private val codec: Codec<String> = mock { }
    private val httpClient: HTTPClientInterface = mock { }

    @Test
    fun urlConfigurationHostWithTrailingSlashUnary() {
        whenever(codec.encodingName()).thenReturn("testing")
        whenever(codec.serialize(any())).thenReturn(Buffer())
        whenever(serializationStrategy.codec<String>(any())).thenReturn(codec)

        val client = createClient("https://connectrpc.com/")
        client.unary(
            "input",
            emptyMap(),
            createMethodSpec(StreamType.UNARY),
        ) { _ -> }
        val captor = argumentCaptor<UnaryHTTPRequest>()
        verify(httpClient).unary(captor.capture(), any())
        assertThat(captor.firstValue.url.toString()).isEqualTo("https://connectrpc.com/com.connectrpc.SomeService/Service")
    }

    @Test
    fun urlConfigurationHostWithoutTrailingSlashUnary() {
        whenever(codec.encodingName()).thenReturn("testing")
        whenever(codec.serialize(any())).thenReturn(Buffer())
        whenever(serializationStrategy.codec<String>(any())).thenReturn(codec)

        val client = createClient("https://connectrpc.com")
        client.unary(
            "input",
            emptyMap(),
            createMethodSpec(StreamType.UNARY),
        ) { _ -> }
        val captor = argumentCaptor<UnaryHTTPRequest>()
        verify(httpClient).unary(captor.capture(), any())
        assertThat(captor.firstValue.url.toString()).isEqualTo("https://connectrpc.com/com.connectrpc.SomeService/Service")
    }

    @Test
    fun urlConfigurationHostWithTrailingSlashStreaming() {
        whenever(codec.encodingName()).thenReturn("testing")
        whenever(codec.serialize(any())).thenReturn(Buffer())
        whenever(serializationStrategy.codec<String>(any())).thenReturn(codec)

        val client = createClient("https://connectrpc.com/")
        CoroutineScope(Dispatchers.IO).launch {
            client.stream(
                emptyMap(),
                createMethodSpec(StreamType.BIDI),
            )
            val captor = argumentCaptor<UnaryHTTPRequest>()
            verify(httpClient).stream(captor.capture(), true, any())
            assertThat(captor.firstValue.url.toString()).isEqualTo("https://connectrpc.com/com.connectrpc.SomeService/Service")
        }
    }

    @Test
    fun urlConfigurationHostWithoutTrailingSlashStreaming() {
        whenever(codec.encodingName()).thenReturn("testing")
        whenever(codec.serialize(any())).thenReturn(Buffer())
        whenever(serializationStrategy.codec<String>(any())).thenReturn(codec)

        val client = createClient("https://connectrpc.com")
        CoroutineScope(Dispatchers.IO).launch {
            client.stream(
                emptyMap(),
                createMethodSpec(StreamType.BIDI),
            )
            val captor = argumentCaptor<HTTPRequest>()
            verify(httpClient).stream(captor.capture(), true, any())
            assertThat(captor.firstValue.url.toString()).isEqualTo("https://connectrpc.com/com.connectrpc.SomeService/Service")
        }
    }

    @Test
    fun finalUrlIsValid() {
        whenever(codec.encodingName()).thenReturn("testing")
        whenever(codec.serialize(any())).thenReturn(Buffer())
        whenever(serializationStrategy.codec<String>(any())).thenReturn(codec)
        val client = createClient("https://connectrpc.com")
        client.unary(
            "",
            emptyMap(),
            createMethodSpec(StreamType.UNARY),
        ) {}
        val captor = argumentCaptor<UnaryHTTPRequest>()
        verify(httpClient).unary(captor.capture(), any())
        assertThat(captor.firstValue.url.toString()).isEqualTo("https://connectrpc.com/com.connectrpc.SomeService/Service")
    }

    @Test
    fun finalUrlIsValidWithHostEndingInSlash() {
        whenever(codec.encodingName()).thenReturn("testing")
        whenever(codec.serialize(any())).thenReturn(Buffer())
        whenever(serializationStrategy.codec<String>(any())).thenReturn(codec)
        val client = createClient("https://connectrpc.com/")
        client.unary(
            "",
            emptyMap(),
            createMethodSpec(StreamType.UNARY),
        ) {}
        val captor = argumentCaptor<UnaryHTTPRequest>()
        verify(httpClient).unary(captor.capture(), any())
        assertThat(captor.firstValue.url.toString()).isEqualTo("https://connectrpc.com/com.connectrpc.SomeService/Service")
    }

    @Test
    fun finalUrlRelativeBaseURI() {
        whenever(codec.encodingName()).thenReturn("testing")
        whenever(codec.serialize(any())).thenReturn(Buffer())
        whenever(serializationStrategy.codec<String>(any())).thenReturn(codec)
        val client = createClient("https://connectrpc.com/api")
        client.unary(
            "",
            emptyMap(),
            createMethodSpec(StreamType.UNARY),
        ) {}
        val captor = argumentCaptor<UnaryHTTPRequest>()
        verify(httpClient).unary(captor.capture(), any())
        assertThat(captor.firstValue.url.toString()).isEqualTo("https://connectrpc.com/api/com.connectrpc.SomeService/Service")
    }

    @Test
    fun finalUrlAbsoluteBaseURI() {
        whenever(codec.encodingName()).thenReturn("testing")
        whenever(codec.serialize(any())).thenReturn(Buffer())
        whenever(serializationStrategy.codec<String>(any())).thenReturn(codec)
        val client = createClient("https://connectrpc.com/api/")
        client.unary(
            "",
            emptyMap(),
            createMethodSpec(StreamType.UNARY),
        ) {}
        val captor = argumentCaptor<UnaryHTTPRequest>()
        verify(httpClient).unary(captor.capture(), any())
        assertThat(captor.firstValue.url.toString()).isEqualTo("https://connectrpc.com/api/com.connectrpc.SomeService/Service")
    }

    @Test
    fun canceledAfterDeadlineIsDeadlineExceeded() {
        // The server can reset the stream at its deadline before the local
        // timer fires; simulate that with a timer that never fires.
        val result = unaryWithCanceledResponse(
            requestTimeout = 10.milliseconds,
            responseDelay = 100.milliseconds,
        )
        assertThat(result.cause.code).isEqualTo(Code.DEADLINE_EXCEEDED)
    }

    @Test
    fun canceledBeforeDeadlineIsCanceled() {
        val result = unaryWithCanceledResponse(
            requestTimeout = 1.hours,
            responseDelay = 10.milliseconds,
        )
        assertThat(result.cause.code).isEqualTo(Code.CANCELED)
    }

    private fun unaryWithCanceledResponse(
        requestTimeout: Duration,
        responseDelay: Duration,
    ): ResponseMessage.Failure<String> {
        whenever(codec.encodingName()).thenReturn("testing")
        whenever(codec.serialize(any())).thenReturn(Buffer())
        whenever(serializationStrategy.codec<String>(any())).thenReturn(codec)
        whenever(httpClient.unary(any(), any())).thenAnswer { invocation ->
            val onResult = invocation.getArgument<(HTTPResponse) -> Unit>(1)
            thread {
                Thread.sleep(responseDelay.inWholeMilliseconds)
                onResult(
                    HTTPResponse(
                        status = null,
                        headers = emptyMap(),
                        message = Buffer(),
                        trailers = emptyMap(),
                        cause = ConnectException(code = Code.CANCELED),
                    ),
                )
            }
            val cancelable: Cancelable = {}
            cancelable
        }
        val neverFiringScheduler = object : Timeout.Scheduler {
            override fun scheduleTimeout(delay: Duration, action: Cancelable): Timeout {
                return Timeout.DEFAULT_SCHEDULER.scheduleTimeout(1.hours, action)
            }
        }
        val client = ProtocolClient(
            httpClient = httpClient,
            config = ProtocolClientConfig(
                host = "https://connectrpc.com/",
                serializationStrategy = serializationStrategy,
                timeoutOracle = { requestTimeout },
                timeoutScheduler = neverFiringScheduler,
            ),
        )
        val latch = CountDownLatch(1)
        var response: ResponseMessage<String>? = null
        client.unary("input", emptyMap(), createMethodSpec(StreamType.UNARY)) { result ->
            response = result
            latch.countDown()
        }
        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue()
        assertThat(response).isInstanceOf(ResponseMessage.Failure::class.java)
        return response as ResponseMessage.Failure<String>
    }

    @Test
    fun streamResultAfterReceiveCloseIsCanceled() = runBlocking {
        val (onResult, httpStream) = mockHTTPStream()
        val stream = createClient("https://connectrpc.com/").stream(
            emptyMap(),
            createMethodSpec(StreamType.SERVER),
        )

        stream.receiveClose()
        // A response frame that was already buffered when the caller canceled.
        onResult(
            StreamResult.Complete(
                ConnectException(code = Code.UNKNOWN, message = "protocol error: status is missing from trailers"),
            ),
        )

        val ex = stream.responseChannel().receiveCatching().exceptionOrNull()
        assertThat(ex).isInstanceOf(ConnectException::class.java)
        assertThat((ex as ConnectException).code).isEqualTo(Code.CANCELED)
        verify(httpStream, atLeastOnce()).receiveClose()
    }

    @Test
    fun streamResultAfterResponseChannelCancelIsIgnored() = runBlocking {
        val (onResult, httpStream) = mockHTTPStream()
        val stream = createClient("https://connectrpc.com/").stream(
            emptyMap(),
            createMethodSpec(StreamType.SERVER),
        )

        stream.responseChannel().cancel()
        onResult(
            StreamResult.Complete(
                cause = ConnectException(code = Code.UNKNOWN, message = "protocol error: status is missing from trailers"),
                trailers = mapOf("key" to listOf("value")),
            ),
        )

        val ex = stream.responseChannel().receiveCatching().exceptionOrNull()
        assertThat(ex).isInstanceOf(CancellationException::class.java)
        assertThat(stream.responseHeaders().await()).isEmpty()
        assertThat(stream.responseTrailers().await()).isEmpty()
        verify(httpStream, atLeastOnce()).receiveClose()
    }

    @Test
    fun receiveCloseAfterCompletionKeepsResult() = runBlocking<Unit> {
        val (onResult, _) = mockHTTPStream()
        val stream = createClient("https://connectrpc.com/").stream(
            emptyMap(),
            createMethodSpec(StreamType.SERVER),
        )

        onResult(
            StreamResult.Complete(
                cause = ConnectException(code = Code.NOT_FOUND, message = "not found"),
                trailers = mapOf("key" to listOf("value")),
            ),
        )
        stream.receiveClose()

        val ex = stream.responseChannel().receiveCatching().exceptionOrNull()
        assertThat(ex).isInstanceOf(ConnectException::class.java)
        assertThat((ex as ConnectException).code).isEqualTo(Code.NOT_FOUND)
        assertThat(stream.responseTrailers().await()["key"]).containsExactly("value")
    }

    private fun mockHTTPStream(): Pair<suspend (StreamResult<Buffer>) -> Unit, Stream> {
        whenever(codec.encodingName()).thenReturn("testing")
        whenever(serializationStrategy.codec<String>(any())).thenReturn(codec)
        val httpStream = mock<Stream>()
        var onResult: (suspend (StreamResult<Buffer>) -> Unit)? = null
        whenever(httpClient.stream(any(), any(), any())).thenAnswer { invocation ->
            onResult = invocation.getArgument(2)
            httpStream
        }
        return Pair({ result -> onResult!!(result) }, httpStream)
    }

    private fun createClient(host: String): ProtocolClient {
        return ProtocolClient(
            httpClient = httpClient,
            config = ProtocolClientConfig(
                host = host,
                serializationStrategy = serializationStrategy,
            ),
        )
    }

    private fun createMethodSpec(streamType: StreamType): MethodSpec<String, String> {
        return MethodSpec(
            path = "com.connectrpc.SomeService/Service",
            String::class,
            String::class,
            streamType,
        )
    }
}

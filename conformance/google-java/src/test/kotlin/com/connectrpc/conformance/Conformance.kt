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

package com.connectrpc.conformance

import com.connectrpc.Code
import com.connectrpc.ConnectError
import com.connectrpc.Headers
import com.connectrpc.ProtocolClientConfig
import com.connectrpc.RequestCompression
import com.connectrpc.StreamResult
import com.connectrpc.Trailers
import com.connectrpc.compression.GzipCompressionPool
import com.connectrpc.conformance.ssl.sslContext
import com.connectrpc.conformance.v1.ErrorDetail
import com.connectrpc.conformance.v1.PayloadType
import com.connectrpc.conformance.v1.TestServiceClient
import com.connectrpc.conformance.v1.UnimplementedServiceClient
import com.connectrpc.conformance.v1.echoStatus
import com.connectrpc.conformance.v1.errorDetail
import com.connectrpc.conformance.v1.payload
import com.connectrpc.conformance.v1.responseParameters
import com.connectrpc.conformance.v1.simpleRequest
import com.connectrpc.conformance.v1.streamingInputCallRequest
import com.connectrpc.conformance.v1.streamingOutputCallRequest
import com.connectrpc.extensions.GoogleJavaProtobufStrategy
import com.connectrpc.getOrThrow
import com.connectrpc.impl.ProtocolClient
import com.connectrpc.okhttp.ConnectOkHttpClient
import com.connectrpc.protocols.NetworkProtocol
import com.google.protobuf.ByteString
import com.google.protobuf.empty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Protocol
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.Before
import org.junit.ClassRule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy
import java.time.Duration
import java.util.Base64
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

@RunWith(Parameterized::class)
class Conformance(
    private val protocol: NetworkProtocol,
    private val serverType: ServerType,
) {
    private lateinit var connectClient: ProtocolClient
    private lateinit var shortTimeoutConnectClient: ProtocolClient
    private lateinit var unimplementedServiceClient: UnimplementedServiceClient
    private lateinit var testServiceConnectClient: TestServiceClient

    companion object {
        private const val CONFORMANCE_VERSION = "0b07f579cb61ad89de24524d62f804a2b03b1acf"

        @JvmStatic
        @Parameters(name = "client={0},server={1}")
        fun data(): Iterable<Array<Any>> {
            return arrayListOf(
                arrayOf(NetworkProtocol.CONNECT, ServerType.CONNECT_GO),
                arrayOf(NetworkProtocol.GRPC, ServerType.CONNECT_GO),
                arrayOf(NetworkProtocol.GRPC_WEB, ServerType.CONNECT_GO),
                arrayOf(NetworkProtocol.GRPC, ServerType.GRPC_GO),
            )
        }

        @JvmField
        @ClassRule
        val CONFORMANCE_CONTAINER_CONNECT = GenericContainer("connectrpc/conformance:$CONFORMANCE_VERSION")
            .withExposedPorts(8080, 8081)
            .withCommand(
                "/usr/local/bin/serverconnect",
                "--h1port",
                "8080",
                "--h2port",
                "8081",
                "--cert",
                "cert/localhost.crt",
                "--key",
                "cert/localhost.key",
            )
            .waitingFor(HostPortWaitStrategy().forPorts(8081))

        @JvmField
        @ClassRule
        val CONFORMANCE_CONTAINER_GRPC = GenericContainer("connectrpc/conformance:$CONFORMANCE_VERSION")
            .withExposedPorts(8081)
            .withCommand(
                "/usr/local/bin/servergrpc",
                "--port",
                "8081",
                "--cert",
                "cert/localhost.crt",
                "--key",
                "cert/localhost.key",
            )
            .waitingFor(HostPortWaitStrategy().forPorts(8081))
    }

    @Before
    fun before() {
        val serverPort = if (serverType == ServerType.CONNECT_GO) CONFORMANCE_CONTAINER_CONNECT.getMappedPort(8081) else CONFORMANCE_CONTAINER_GRPC.getMappedPort(8081)
        val host = "https://localhost:$serverPort"
        val (sslSocketFactory, trustManager) = sslContext()
        val client = OkHttpClient.Builder()
            .protocols(listOf(Protocol.HTTP_2, Protocol.HTTP_1_1))
            .connectTimeout(Duration.ofMinutes(1))
            .readTimeout(Duration.ofMinutes(1))
            .writeTimeout(Duration.ofMinutes(1))
            .callTimeout(Duration.ofMinutes(1))
            .sslSocketFactory(sslSocketFactory, trustManager)
            .build()
        shortTimeoutConnectClient = ProtocolClient(
            httpClient = ConnectOkHttpClient(
                client.newBuilder()
                    .connectTimeout(Duration.ofMillis(1))
                    .readTimeout(Duration.ofMillis(1))
                    .writeTimeout(Duration.ofMillis(1))
                    .callTimeout(Duration.ofMillis(1))
                    .build(),
            ),
            ProtocolClientConfig(
                host = host,
                serializationStrategy = GoogleJavaProtobufStrategy(),
                networkProtocol = protocol,
                requestCompression = RequestCompression(10, GzipCompressionPool),
                compressionPools = listOf(GzipCompressionPool),
            ),
        )
        connectClient = ProtocolClient(
            httpClient = ConnectOkHttpClient(client),
            ProtocolClientConfig(
                host = host,
                serializationStrategy = GoogleJavaProtobufStrategy(),
                networkProtocol = protocol,
                requestCompression = RequestCompression(10, GzipCompressionPool),
                compressionPools = listOf(GzipCompressionPool),
            ),
        )
        testServiceConnectClient = TestServiceClient(connectClient)
        unimplementedServiceClient = UnimplementedServiceClient(connectClient)
    }

    @Test
    fun failServerStreaming(): Unit = runBlocking {
        val expectedErrorDetail = errorDetail {
            reason = "soirée 🎉"
            domain = "connect-conformance"
        }
        val stream = testServiceConnectClient.failStreamingOutputCall()
        val sizes = listOf(
            31415,
            9,
            2653,
            58979,
        )
        val parameters = sizes.mapIndexed { index, value ->
            responseParameters {
                size = value
                intervalUs = index * 10
            }
        }

        stream.sendAndClose(
            streamingOutputCallRequest {
                responseParameters.addAll(parameters)
            },
        )
        val countDownLatch = CountDownLatch(1)
        withContext(Dispatchers.IO) {
            val job = async {
                try {
                    val result = streamResults(stream.resultChannel())
                    assertThat(result.messages.map { it.payload.body.size() }).isEqualTo(sizes)
                    assertThat(result.code).isEqualTo(Code.RESOURCE_EXHAUSTED)
                    assertThat(result.error).isInstanceOf(ConnectError::class.java)
                    val connectError = result.error as ConnectError
                    assertThat(connectError.code).isEqualTo(Code.RESOURCE_EXHAUSTED)
                    assertThat(connectError.message).isEqualTo("soirée 🎉")
                    assertThat(connectError.unpackedDetails(ErrorDetail::class)).containsExactly(
                        expectedErrorDetail,
                    )
                } finally {
                    countDownLatch.countDown()
                }
            }
            countDownLatch.await(5, TimeUnit.SECONDS)
            job.cancel()
            assertThat(countDownLatch.count).isZero()
        }
    }

    @Test
    fun emptyUnary(): Unit = runBlocking {
        val countDownLatch = CountDownLatch(1)
        testServiceConnectClient.emptyCall(empty {}) { response ->
            response.failure {
                fail<Unit>("expected error to be null")
            }
            response.success { success ->
                assertThat(success.message).isEqualTo(empty {})
                countDownLatch.countDown()
            }
        }
        countDownLatch.await(500, TimeUnit.MILLISECONDS)
        assertThat(countDownLatch.count).isZero()
    }

    @Test
    fun largeUnary(): Unit = runBlocking {
        val size = 314159
        val message = simpleRequest {
            responseSize = size
            payload = payload {
                body = ByteString.copyFrom(ByteArray(size))
            }
        }
        val countDownLatch = CountDownLatch(1)
        testServiceConnectClient.unaryCall(message) { response ->
            response.failure {
                fail<Unit>("expected error to be null")
            }
            response.success { success ->
                assertThat(success.message.payload?.body?.toByteArray()?.size).isEqualTo(size)
                countDownLatch.countDown()
            }
        }
        countDownLatch.await(500, TimeUnit.MILLISECONDS)
        assertThat(countDownLatch.count).isZero()
    }

    @Test
    fun customMetadata(): Unit = runBlocking {
        val size = 314159
        val leadingKey = "x-grpc-test-echo-initial"
        val leadingValue = "test_initial_metadata_value"
        val trailingKey = "x-grpc-test-echo-trailing-bin"
        val trailingValue = byteArrayOf(0xab.toByte(), 0xab.toByte(), 0xab.toByte())
        val headers =
            mapOf(
                leadingKey to listOf(leadingValue),
                trailingKey to listOf(b64Encode(trailingValue)),
            )
        val message = simpleRequest {
            responseSize = size
            payload = payload { body = ByteString.copyFrom(ByteArray(size)) }
        }
        val countDownLatch = CountDownLatch(1)
        testServiceConnectClient.unaryCall(message, headers) { response ->
            assertThat(response.code).isEqualTo(Code.OK)
            assertThat(response.headers[leadingKey]).containsExactly(leadingValue)
            assertThat(response.trailers[trailingKey]).containsExactly(b64Encode(trailingValue))
            response.failure {
                fail<Unit>("expected error to be null")
            }
            response.success { success ->
                assertThat(success.message.payload!!.body!!.size()).isEqualTo(size)
                countDownLatch.countDown()
            }
        }
        countDownLatch.await(500, TimeUnit.MILLISECONDS)
        assertThat(countDownLatch.count).isZero()
    }

    @Test
    fun statusCodeAndMessage(): Unit = runBlocking {
        val message = simpleRequest {
            responseStatus = echoStatus {
                code = Code.UNKNOWN.value
                message = "test status message"
            }
        }
        val countDownLatch = CountDownLatch(1)
        testServiceConnectClient.unaryCall(message) { response ->
            assertThat(response.code).isEqualTo(Code.UNKNOWN)
            response.failure { errorResponse ->
                assertThat(errorResponse.error).isNotNull()
                assertThat(errorResponse.code).isEqualTo(Code.UNKNOWN)
                assertThat(errorResponse.error.message).isEqualTo("test status message")
                countDownLatch.countDown()
            }
            response.success {
                fail<Unit>("unexpected success")
            }
        }

        countDownLatch.await(500, TimeUnit.MILLISECONDS)
        assertThat(countDownLatch.count).isZero()
    }

    @Test
    fun timeoutOnSleepingServer(): Unit = runBlocking {
        val countDownLatch = CountDownLatch(1)
        val client = TestServiceClient(shortTimeoutConnectClient)
        val request = streamingOutputCallRequest {
            payload = payload {
                body = ByteString.copyFrom(ByteArray(271828))
            }
            responseParameters.add(
                responseParameters {
                    size = 31415
                    intervalUs = 50_000
                },
            )
        }
        val stream = client.streamingOutputCall()
        withContext(Dispatchers.IO) {
            val job = async {
                try {
                    val result = streamResults(stream.resultChannel())
                    assertThat(result.error).isInstanceOf(ConnectError::class.java)
                    val connectErr = result.error as ConnectError
                    assertThat(connectErr.code).isEqualTo(Code.DEADLINE_EXCEEDED)
                    assertThat(result.code).isEqualTo(Code.DEADLINE_EXCEEDED)
                } finally {
                    countDownLatch.countDown()
                }
            }
            stream.sendAndClose(request)
            countDownLatch.await(5, TimeUnit.SECONDS)
            job.cancel()
            assertThat(countDownLatch.count).isZero()
        }
    }

    @Test
    fun specialStatus(): Unit = runBlocking {
        val statusMessage =
            "\\t\\ntest with whitespace\\r\\nand Unicode BMP ☺ and non-BMP \uD83D\uDE08\\t\\n"
        val countDownLatch = CountDownLatch(1)
        testServiceConnectClient.unaryCall(
            simpleRequest {
                responseStatus = echoStatus {
                    code = 2
                    message = statusMessage
                }
            },
        ) { response ->
            response.failure { errorResponse ->
                val error = errorResponse.error
                assertThat(error.code).isEqualTo(Code.UNKNOWN)
                assertThat(response.code).isEqualTo(Code.UNKNOWN)
                assertThat(error.message).isEqualTo(statusMessage)
                countDownLatch.countDown()
            }
            response.success {
                fail<Unit>("unexpected success")
            }
        }
        countDownLatch.await(500, TimeUnit.MILLISECONDS)
        assertThat(countDownLatch.count).isZero()
    }

    @Test
    fun unimplementedMethod(): Unit = runBlocking {
        val countDownLatch = CountDownLatch(1)
        testServiceConnectClient.unimplementedCall(empty {}) { response ->
            assertThat(response.code).isEqualTo(Code.UNIMPLEMENTED)
            countDownLatch.countDown()
        }
        countDownLatch.await(500, TimeUnit.MILLISECONDS)
        assertThat(countDownLatch.count).isZero()
    }

    @Test
    fun unimplementedService(): Unit = runBlocking {
        val countDownLatch = CountDownLatch(1)
        unimplementedServiceClient.unimplementedCall(empty {}) { response ->
            assertThat(response.code).isEqualTo(Code.UNIMPLEMENTED)
            countDownLatch.countDown()
        }
        countDownLatch.await(500, TimeUnit.MILLISECONDS)
        assertThat(countDownLatch.count).isZero()
    }

    @Test
    fun unimplementedServerStreamingService(): Unit = runBlocking {
        val countDownLatch = CountDownLatch(1)
        val stream = unimplementedServiceClient.unimplementedStreamingOutputCall()
        stream.sendAndClose(empty { })
        withContext(Dispatchers.IO) {
            val job = async {
                try {
                    val result = streamResults(stream.resultChannel())
                    assertThat(result.code).isEqualTo(Code.UNIMPLEMENTED)
                    assertThat(result.error).isInstanceOf(ConnectError::class.java)
                    val connectErr = result.error as ConnectError
                    assertThat(connectErr.code).isEqualTo(Code.UNIMPLEMENTED)
                } finally {
                    countDownLatch.countDown()
                }
            }
            countDownLatch.await(5, TimeUnit.SECONDS)
            job.cancel()
            assertThat(countDownLatch.count).isZero()
        }
    }

    @Test
    fun failUnary(): Unit = runBlocking {
        val expectedErrorDetail = errorDetail {
            reason = "soirée 🎉"
            domain = "connect-conformance"
        }
        val countDownLatch = CountDownLatch(1)
        testServiceConnectClient.failUnaryCall(simpleRequest {}) { response ->
            assertThat(response.code).isEqualTo(Code.RESOURCE_EXHAUSTED)
            response.failure { errorResponse ->
                val error = errorResponse.error
                assertThat(error.code).isEqualTo(Code.RESOURCE_EXHAUSTED)
                assertThat(error.message).isEqualTo("soirée 🎉")
                val connectErrorDetails = error.unpackedDetails(ErrorDetail::class)
                assertThat(connectErrorDetails).containsExactly(expectedErrorDetail)
                countDownLatch.countDown()
            }
            response.success {
                fail<Unit>("unexpected success")
            }
        }
        countDownLatch.await(500, TimeUnit.MILLISECONDS)
        assertThat(countDownLatch.count).isZero()
    }

    @Test
    fun emptyUnaryBlocking(): Unit = runBlocking {
        val response = testServiceConnectClient.emptyCallBlocking(empty {}).execute()
        response.failure {
            fail<Unit>("expected error to be null")
        }
        response.success { success ->
            assertThat(success.message).isEqualTo(empty {})
        }
    }

    @Test
    fun largeUnaryBlocking(): Unit = runBlocking {
        val size = 314159
        val message = simpleRequest {
            responseSize = size
            payload = payload {
                body = ByteString.copyFrom(ByteArray(size))
            }
        }
        val response = testServiceConnectClient.unaryCallBlocking(message).execute()
        response.failure {
            fail<Unit>("expected error to be null")
        }
        response.success { success ->
            assertThat(success.message.payload?.body?.toByteArray()?.size).isEqualTo(size)
        }
    }

    @Test
    fun customMetadataBlocking(): Unit = runBlocking {
        val size = 314159
        val leadingKey = "x-grpc-test-echo-initial"
        val leadingValue = "test_initial_metadata_value"
        val trailingKey = "x-grpc-test-echo-trailing-bin"
        val trailingValue = byteArrayOf(0xab.toByte(), 0xab.toByte(), 0xab.toByte())
        val headers =
            mapOf(
                leadingKey to listOf(leadingValue),
                trailingKey to listOf(b64Encode(trailingValue)),
            )
        val message = simpleRequest {
            responseSize = size
            payload = payload { body = ByteString.copyFrom(ByteArray(size)) }
        }
        val response = testServiceConnectClient.unaryCallBlocking(message, headers).execute()
        assertThat(response.code).isEqualTo(Code.OK)
        assertThat(response.headers[leadingKey]).containsExactly(leadingValue)
        assertThat(response.trailers[trailingKey]).containsExactly(b64Encode(trailingValue))
        response.failure {
            fail<Unit>("expected error to be null")
        }
        response.success { success ->
            assertThat(success.message.payload!!.body!!.size()).isEqualTo(size)
        }
    }

    @Test
    fun statusCodeAndMessageBlocking(): Unit = runBlocking {
        val message = simpleRequest {
            responseStatus = echoStatus {
                code = Code.UNKNOWN.value
                message = "test status message"
            }
        }
        val response = testServiceConnectClient.unaryCallBlocking(message).execute()
        assertThat(response.code).isEqualTo(Code.UNKNOWN)
        response.failure { errorResponse ->
            assertThat(errorResponse.error).isNotNull()
            assertThat(errorResponse.code).isEqualTo(Code.UNKNOWN)
            assertThat(errorResponse.error.message).isEqualTo("test status message")
        }
        response.success {
            fail<Unit>("unexpected success")
        }
    }

    @Test
    fun specialStatusBlocking(): Unit = runBlocking {
        val statusMessage =
            "\\t\\ntest with whitespace\\r\\nand Unicode BMP ☺ and non-BMP \uD83D\uDE08\\t\\n"
        val response = testServiceConnectClient.unaryCallBlocking(
            simpleRequest {
                responseStatus = echoStatus {
                    code = 2
                    message = statusMessage
                }
            },
        ).execute()
        response.failure { errorResponse ->
            val error = errorResponse.error
            assertThat(error.code).isEqualTo(Code.UNKNOWN)
            assertThat(response.code).isEqualTo(Code.UNKNOWN)
            assertThat(error.message).isEqualTo(statusMessage)
        }
        response.success {
            fail<Unit>("unexpected success")
        }
    }

    @Test
    fun unimplementedMethodBlocking(): Unit = runBlocking {
        val response = testServiceConnectClient.unimplementedCallBlocking(empty {}).execute()
        assertThat(response.code).isEqualTo(Code.UNIMPLEMENTED)
    }

    @Test
    fun unimplementedServiceBlocking(): Unit = runBlocking {
        val response = unimplementedServiceClient.unimplementedCallBlocking(empty {}).execute()
        assertThat(response.code).isEqualTo(Code.UNIMPLEMENTED)
    }

    @Test
    fun failUnaryBlocking(): Unit = runBlocking {
        val expectedErrorDetail = errorDetail {
            reason = "soirée 🎉"
            domain = "connect-conformance"
        }
        val response = testServiceConnectClient.failUnaryCallBlocking(simpleRequest {}).execute()
        assertThat(response.code).isEqualTo(Code.RESOURCE_EXHAUSTED)
        response.failure { errorResponse ->
            val error = errorResponse.error
            assertThat(error.code).isEqualTo(Code.RESOURCE_EXHAUSTED)
            assertThat(error.message).isEqualTo("soirée 🎉")
            val connectErrorDetails = error.unpackedDetails(ErrorDetail::class)
            assertThat(connectErrorDetails).containsExactly(expectedErrorDetail)
        }
        response.success {
            fail<Unit>("unexpected success")
        }
    }

    @Test
    fun emptyUnaryCallback(): Unit = runBlocking {
        val countDownLatch = CountDownLatch(1)
        testServiceConnectClient.emptyCall(empty {}) { response ->
            response.failure {
                fail<Unit>("expected error to be null")
            }
            response.success { success ->
                assertThat(success.message).isEqualTo(empty {})
                countDownLatch.countDown()
            }
        }
        countDownLatch.await(500, TimeUnit.MILLISECONDS)
        assertThat(countDownLatch.count).isZero()
    }

    @Test
    fun largeUnaryCallback(): Unit = runBlocking {
        val size = 314159
        val message = simpleRequest {
            responseSize = size
            payload = payload {
                body = ByteString.copyFrom(ByteArray(size))
            }
        }
        val countDownLatch = CountDownLatch(1)
        testServiceConnectClient.unaryCall(message) { response ->
            response.failure {
                fail<Unit>("expected error to be null")
            }
            response.success { success ->
                assertThat(success.message.payload?.body?.toByteArray()?.size).isEqualTo(size)
                countDownLatch.countDown()
            }
        }
        countDownLatch.await(500, TimeUnit.MILLISECONDS)
        assertThat(countDownLatch.count).isZero()
    }

    @Test
    fun customMetadataCallback(): Unit = runBlocking {
        val size = 314159
        val leadingKey = "x-grpc-test-echo-initial"
        val leadingValue = "test_initial_metadata_value"
        val trailingKey = "x-grpc-test-echo-trailing-bin"
        val trailingValue = byteArrayOf(0xab.toByte(), 0xab.toByte(), 0xab.toByte())
        val headers =
            mapOf(
                leadingKey to listOf(leadingValue),
                trailingKey to listOf(b64Encode(trailingValue)),
            )
        val message = simpleRequest {
            responseSize = size
            payload = payload { body = ByteString.copyFrom(ByteArray(size)) }
        }
        val countDownLatch = CountDownLatch(1)
        testServiceConnectClient.unaryCall(message, headers) { response ->
            assertThat(response.code).isEqualTo(Code.OK)
            assertThat(response.headers[leadingKey]).containsExactly(leadingValue)
            assertThat(response.trailers[trailingKey]).containsExactly(b64Encode(trailingValue))
            response.failure {
                fail<Unit>("expected error to be null")
            }
            response.success { success ->
                assertThat(success.message.payload!!.body!!.size()).isEqualTo(size)
                countDownLatch.countDown()
            }
        }
        countDownLatch.await(500, TimeUnit.MILLISECONDS)
        assertThat(countDownLatch.count).isZero()
    }

    @Test
    fun statusCodeAndMessageCallback(): Unit = runBlocking {
        val message = simpleRequest {
            responseStatus = echoStatus {
                code = Code.UNKNOWN.value
                message = "test status message"
            }
        }
        val countDownLatch = CountDownLatch(1)
        testServiceConnectClient.unaryCall(message) { response ->
            assertThat(response.code).isEqualTo(Code.UNKNOWN)
            response.failure { errorResponse ->
                assertThat(errorResponse.error).isNotNull()
                assertThat(errorResponse.code).isEqualTo(Code.UNKNOWN)
                assertThat(errorResponse.error.message).isEqualTo("test status message")
                countDownLatch.countDown()
            }
            response.success {
                fail<Unit>("unexpected success")
            }
        }

        countDownLatch.await(500, TimeUnit.MILLISECONDS)
        assertThat(countDownLatch.count).isZero()
    }

    @Test
    fun specialStatusCallback(): Unit = runBlocking {
        val statusMessage =
            "\\t\\ntest with whitespace\\r\\nand Unicode BMP ☺ and non-BMP \uD83D\uDE08\\t\\n"
        val countDownLatch = CountDownLatch(1)
        testServiceConnectClient.unaryCall(
            simpleRequest {
                responseStatus = echoStatus {
                    code = 2
                    message = statusMessage
                }
            },
        ) { response ->
            response.failure { errorResponse ->
                val error = errorResponse.error
                assertThat(error.code).isEqualTo(Code.UNKNOWN)
                assertThat(response.code).isEqualTo(Code.UNKNOWN)
                assertThat(error.message).isEqualTo(statusMessage)
                countDownLatch.countDown()
            }
            response.success {
                fail<Unit>("unexpected success")
            }
        }
        countDownLatch.await(500, TimeUnit.MILLISECONDS)
        assertThat(countDownLatch.count).isZero()
    }

    @Test
    fun unimplementedMethodCallback(): Unit = runBlocking {
        val countDownLatch = CountDownLatch(1)
        testServiceConnectClient.unimplementedCall(empty {}) { response ->
            assertThat(response.code).isEqualTo(Code.UNIMPLEMENTED)
            countDownLatch.countDown()
        }
        countDownLatch.await(500, TimeUnit.MILLISECONDS)
        assertThat(countDownLatch.count).isZero()
    }

    @Test
    fun unimplementedServiceCallback(): Unit = runBlocking {
        val countDownLatch = CountDownLatch(1)
        unimplementedServiceClient.unimplementedCall(empty {}) { response ->
            assertThat(response.code).isEqualTo(Code.UNIMPLEMENTED)
            countDownLatch.countDown()
        }
        countDownLatch.await(500, TimeUnit.MILLISECONDS)
        assertThat(countDownLatch.count).isZero()
    }

    @Test
    fun failUnaryCallback(): Unit = runBlocking {
        val expectedErrorDetail = errorDetail {
            reason = "soirée 🎉"
            domain = "connect-conformance"
        }
        val countDownLatch = CountDownLatch(1)
        testServiceConnectClient.failUnaryCall(simpleRequest {}) { response ->
            assertThat(response.code).isEqualTo(Code.RESOURCE_EXHAUSTED)
            response.failure { errorResponse ->
                val error = errorResponse.error
                assertThat(error.code).isEqualTo(Code.RESOURCE_EXHAUSTED)
                assertThat(error.message).isEqualTo("soirée 🎉")
                val connectErrorDetails = error.unpackedDetails(ErrorDetail::class)
                assertThat(connectErrorDetails).containsExactly(expectedErrorDetail)
                countDownLatch.countDown()
            }
            response.success {
                fail<Unit>("unexpected success")
            }
        }
        countDownLatch.await(500, TimeUnit.MILLISECONDS)
        assertThat(countDownLatch.count).isZero()
    }

    @Test
    fun clientStreaming(): Unit = runBlocking {
        val stream = testServiceConnectClient.streamingInputCall(emptyMap())
        var sum = 0
        listOf(256000, 8, 1024, 32768).forEach { size ->
            stream.send(
                streamingInputCallRequest {
                    payload = payload {
                        type = PayloadType.COMPRESSABLE
                        body = ByteString.copyFrom(ByteArray(size))
                    }
                },
            ).getOrThrow()
            sum += size
        }
        val countDownLatch = CountDownLatch(1)
        withContext(Dispatchers.IO) {
            val job = async {
                try {
                    val result = stream.receiveAndClose().getOrThrow()
                    assertThat(result.aggregatedPayloadSize).isEqualTo(sum)
                } finally {
                    countDownLatch.countDown()
                }
            }
            countDownLatch.await(5, TimeUnit.MINUTES)
            job.cancel()
            assertThat(countDownLatch.count).isZero()
        }
    }

    private data class ServerStreamingResult<Output>(
        val headers: Headers,
        val messages: List<Output>,
        val code: Code,
        val trailers: Trailers,
        val error: Throwable?,
    )

    /*
     * Convenience method to return all results (with sanity checking) for calls which stream results from the server
     * (bidi and server streaming).
     *
     * This allows us to easily verify headers, messages, trailers, and errors without having to use fold/maybeFold
     * manually in each location.
     */
    private suspend fun <Output> streamResults(channel: ReceiveChannel<StreamResult<Output>>): ServerStreamingResult<Output> {
        val seenHeaders = AtomicBoolean(false)
        var headers: Headers = emptyMap()
        val messages: MutableList<Output> = mutableListOf()
        val seenCompletion = AtomicBoolean(false)
        var code: Code = Code.UNKNOWN
        var trailers: Headers = emptyMap()
        var error: Throwable? = null
        for (response in channel) {
            response.maybeFold(
                onHeaders = {
                    if (!seenHeaders.compareAndSet(false, true)) {
                        throw IllegalStateException("multiple onHeaders callbacks")
                    }
                    headers = it.headers
                },
                onMessage = {
                    messages.add(it.message)
                },
                onCompletion = {
                    if (!seenCompletion.compareAndSet(false, true)) {
                        throw IllegalStateException("multiple onCompletion callbacks")
                    }
                    code = it.code
                    trailers = it.trailers
                    error = it.error
                },
            )
        }
        if (!seenCompletion.get()) {
            throw IllegalStateException("didn't get completion message")
        }
        return ServerStreamingResult(headers, messages, code, trailers, error)
    }

    private fun b64Encode(trailingValue: ByteArray): String {
        return String(Base64.getEncoder().encode(trailingValue))
    }
}

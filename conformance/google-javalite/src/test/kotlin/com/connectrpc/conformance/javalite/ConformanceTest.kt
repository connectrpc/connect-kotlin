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

package com.connectrpc.conformance.javalite

import com.connectrpc.Code
import com.connectrpc.ConnectException
import com.connectrpc.conformance.BaseConformanceTest
import com.connectrpc.conformance.ServerType
import com.connectrpc.conformance.v1.ErrorDetail
import com.connectrpc.conformance.v1.PayloadType
import com.connectrpc.conformance.v1.StreamingOutputCallResponse
import com.connectrpc.conformance.v1.TestServiceClient
import com.connectrpc.conformance.v1.UnimplementedServiceClient
import com.connectrpc.conformance.v1.echoStatus
import com.connectrpc.conformance.v1.errorDetail
import com.connectrpc.conformance.v1.payload
import com.connectrpc.conformance.v1.responseParameters
import com.connectrpc.conformance.v1.simpleRequest
import com.connectrpc.conformance.v1.streamingInputCallRequest
import com.connectrpc.conformance.v1.streamingOutputCallRequest
import com.connectrpc.extensions.GoogleJavaLiteProtobufStrategy
import com.connectrpc.getOrThrow
import com.connectrpc.protocols.NetworkProtocol
import com.google.protobuf.ByteString
import com.google.protobuf.empty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(Parameterized::class)
class ConformanceTest(
    protocol: NetworkProtocol,
    serverType: ServerType,
): BaseConformanceTest(protocol, serverType) {

    private lateinit var unimplementedServiceClient: UnimplementedServiceClient
    private lateinit var testServiceConnectClient: TestServiceClient

    @Before
    fun before() {
        init(GoogleJavaLiteProtobufStrategy())
        testServiceConnectClient = TestServiceClient(connectClient)
        unimplementedServiceClient = UnimplementedServiceClient(connectClient)
    }

    @Test
    fun serverStreaming(): Unit = runBlocking {
        val sizes = listOf(512_000, 16, 2_028, 65_536)
        val stream = testServiceConnectClient.streamingOutputCall()
        val params = sizes.map { responseParameters { size = it } }.toList()
        stream.sendAndClose(
            streamingOutputCallRequest {
                responseType = PayloadType.COMPRESSABLE
                responseParameters += params
            },
        ).getOrThrow()
        val responses = mutableListOf<StreamingOutputCallResponse>()
        for (response in stream.responseChannel()) {
            responses.add(response)
        }
        assertThat(responses.map { it.payload.type }.toSet()).isEqualTo(setOf(PayloadType.COMPRESSABLE))
        assertThat(responses.map { it.payload.body.size() }).isEqualTo(sizes)
    }

    @Test
    fun pingPong(): Unit = runBlocking {
        val stream = testServiceConnectClient.fullDuplexCall()
        val responseChannel = stream.responseChannel()
        listOf(512_000, 16, 2_028, 65_536).forEach {
            val param = responseParameters { size = it }
            stream.send(
                streamingOutputCallRequest {
                    responseType = PayloadType.COMPRESSABLE
                    responseParameters += param
                },
            ).getOrThrow()
            val response = responseChannel.receive()
            val payload = response.payload
            assertThat(payload.type).isEqualTo(PayloadType.COMPRESSABLE)
            assertThat(payload.body).hasSize(it)
        }
        stream.sendClose()
        // We've already read all the messages
        assertThat(responseChannel.receiveCatching().isClosed).isTrue()
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
                val responses = mutableListOf<StreamingOutputCallResponse>()
                try {
                    for (response in stream.responseChannel()) {
                        responses.add(response)
                    }
                    fail("expected call to fail with ConnectException")
                } catch (e: ConnectException) {
                    assertThat(responses.map { it.payload.body.size() }).isEqualTo(sizes)
                    assertThat(e.code).isEqualTo(Code.RESOURCE_EXHAUSTED)
                    assertThat(e.message).isEqualTo("soirée 🎉")
                    assertThat(e.unpackedDetails(ErrorDetail::class)).containsExactly(expectedErrorDetail)
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
        val response = testServiceConnectClient.emptyCall(empty {}).getOrThrow()
        assertThat(response).isEqualTo(empty {})
    }

    @Test
    fun largeUnary(): Unit = runBlocking {
        val size = 314159
        val message = simpleRequest {
            responseType = PayloadType.COMPRESSABLE
            responseSize = size
            payload = payload {
                body = ByteString.copyFrom(ByteArray(size))
            }
        }
        val response = testServiceConnectClient.unaryCall(message).getOrThrow()
        assertThat(response.payload.body.toByteArray()).hasSize(size)
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
                assertThat(errorResponse.cause).isNotNull()
                assertThat(errorResponse.code).isEqualTo(Code.UNKNOWN)
                assertThat(errorResponse.cause.message).isEqualTo("test status message")
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
            val job = launch {
                try {
                    stream.responseChannel().receive()
                    fail("unexpected ConnectException to be thrown")
                } catch (e: ConnectException) {
                    assertThat(e.code)
                        .withFailMessage { "Expected Code.DEADLINE_EXCEEDED but got ${e.code}" }
                        .isEqualTo(Code.DEADLINE_EXCEEDED)
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
                val error = errorResponse.cause
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
                    stream.responseChannel().receive()
                    fail("expected call to fail with a ConnectException")
                } catch (e: ConnectException) {
                    assertThat(e.code).isEqualTo(Code.UNIMPLEMENTED)
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
                val error = errorResponse.cause
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
            assertThat(errorResponse.cause).isNotNull()
            assertThat(errorResponse.code).isEqualTo(Code.UNKNOWN)
            assertThat(errorResponse.cause.message).isEqualTo("test status message")
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
            val error = errorResponse.cause
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
            val error = errorResponse.cause
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
                assertThat(errorResponse.cause).isNotNull()
                assertThat(errorResponse.code).isEqualTo(Code.UNKNOWN)
                assertThat(errorResponse.cause.message).isEqualTo("test status message")
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
                val error = errorResponse.cause
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
                val error = errorResponse.cause
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
                    val response = stream.receiveAndClose()
                    assertThat(response.aggregatedPayloadSize).isEqualTo(sum)
                } finally {
                    countDownLatch.countDown()
                }
            }
            countDownLatch.await(5, TimeUnit.SECONDS)
            job.cancel()
            assertThat(countDownLatch.count).isZero()
        }
    }
}
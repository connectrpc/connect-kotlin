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

package com.connectrpc.crosstest

import com.connectrpc.Code
import com.connectrpc.crosstest.ssl.TestSuite
import com.connectrpc.impl.ProtocolClient
import com.google.protobuf.ByteString
import com.grpc.testing.ErrorDetail
import com.grpc.testing.TestServiceClient
import com.grpc.testing.UnimplementedServiceClient
import com.grpc.testing.echoStatus
import com.grpc.testing.empty
import com.grpc.testing.errorDetail
import com.grpc.testing.payload
import com.grpc.testing.responseParameters
import com.grpc.testing.simpleRequest
import com.grpc.testing.streamingOutputCallRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import okio.ByteString.Companion.toByteString
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.system.measureTimeMillis

class TestServiceClientSuite(
    client: ProtocolClient,
    private val shortTimeoutClient: ProtocolClient
) : TestSuite {
    private val testServiceConnectClient = TestServiceClient(client)
    private val unimplementedServiceClient = UnimplementedServiceClient(client)

    private val tests = mutableListOf<Pair<String, suspend () -> Unit>>()

    override suspend fun test(tag: String) {
        println()
        tests.forEachIndexed { index, (testName, test) ->
            print("[$tag] Executing test case ${index + 1}/${tests.size}: $testName")
            val millis = measureTimeMillis {
                test()
            }
            println("  [$millis ms]")
        }
    }
    private fun register(testName: String, test: suspend () -> Unit) {
        tests.add(testName to test)
    }

    override suspend fun emptyUnary() = register("empty_unary") {
        val response = testServiceConnectClient.emptyCall(empty {})
        response.failure {
            fail<Unit>("expected error to be null")
        }
        response.success { success ->
            assertThat(success.message).isEqualTo(empty {})
        }
    }

    override suspend fun largeUnary() = register("large_unary") {
        val size = 314159
        val message = simpleRequest {
            responseSize = size
            payload = payload {
                body = ByteString.copyFrom(ByteArray(size))
            }
        }
        val response = testServiceConnectClient.unaryCall(message)
        response.failure {
            fail<Unit>("expected error to be null", it.error)
        }
        response.success { success ->
            assertThat(success.message.payload?.body?.toByteArray()?.size).isEqualTo(size)
        }
    }

    override suspend fun serverStreaming() = register("server_streaming") {
        val sizes = listOf(31415, 9, 2653, 58979)
        val countDownLatch = CountDownLatch(1)
        var responseCount = 0
        val stream = testServiceConnectClient.streamingOutputCall()
        withContext(Dispatchers.IO) {
            val job = async {
                for (res in stream.resultChannel()) {
                    res.maybeFold(
                        onMessage = { result ->
                            val message = result.message
                            assertThat(result.error).isNull()
                            assertThat(message.payload).isNotNull()
                            assertThat(message.payload!!.body!!.size()).isEqualTo(sizes[responseCount])
                            responseCount++
                        },
                        onCompletion = {
                            countDownLatch.countDown()
                        }
                    )
                }
            }
            val parameters = sizes.mapIndexed { index, len ->
                responseParameters {
                    size = len
                    intervalUs = index * 10
                }
            }
            stream.send(
                streamingOutputCallRequest {
                    responseParameters.addAll(parameters)
                }
            )
            countDownLatch.await(5, TimeUnit.SECONDS)
            job.cancel()
            assertThat(countDownLatch.count).isZero()
            assertThat(responseCount).isEqualTo(4)
            stream.close()
        }
    }

    override suspend fun emptyStream() = register("empty_stream") {
        val countDownLatch = CountDownLatch(1)
        val stream = testServiceConnectClient.streamingOutputCall()
        withContext(Dispatchers.IO) {
            val job = async {
                for (res in stream.resultChannel()) {
                    res.maybeFold(
                        onCompletion = { result ->
                            assertThat(result.error).isNull()
                            countDownLatch.countDown()
                        }
                    )
                }
            }
            stream.send(
                streamingOutputCallRequest {
                    responseParameters.addAll(emptyList())
                }
            )
            countDownLatch.await(5, TimeUnit.SECONDS)
            job.cancel()
            assertThat(countDownLatch.count).isZero()
            stream.close()
        }
    }

    override suspend fun customMetadata() = register("custom_metadata") {
        val size = 314159
        val leadingKey = "x-grpc-test-echo-initial"
        val leadingValue = "test_initial_metadata_value"
        val trailingKey = "x-grpc-test-echo-trailing-bin"
        val trailingValue = byteArrayOf(0xab.toByte(), 0xab.toByte(), 0xab.toByte())
        val headers =
            mapOf(
                leadingKey to listOf(leadingValue),
                trailingKey to listOf(trailingValue.b64Encode())
            )
        val message = simpleRequest {
            responseSize = size
            payload = payload { body = ByteString.copyFrom(ByteArray(size)) }
        }
        val response = testServiceConnectClient.unaryCall(message, headers)
        assertThat(response.code).isEqualTo(Code.OK)
        assertThat(response.headers[leadingKey]).containsExactly(leadingValue)
        assertThat(response.trailers[trailingKey]).containsExactly(trailingValue.b64Encode())
        response.failure {
            fail<Unit>("expected error to be null")
        }
        response.success { success ->
            assertThat(success.message.payload!!.body!!.size()).isEqualTo(size)
        }
    }

    override suspend fun customMetadataServerStreaming() = register("custom_metadata_server_streaming") {
        val leadingKey = "x-grpc-test-echo-initial"
        val leadingValue = "test_initial_metadata_value"
        val trailingKey = "x-grpc-test-echo-trailing-bin"
        val trailingValue = byteArrayOf(0xab.toByte(), 0xab.toByte(), 0xab.toByte())
        val size = 31415
        val countDownLatch = CountDownLatch(3)
        val stream = testServiceConnectClient.streamingOutputCall(
            mapOf(
                leadingKey to listOf(leadingValue),
                trailingKey to listOf(trailingValue.b64Encode())
            )
        )
        withContext(Dispatchers.IO) {
            val job = async {
                for (res in stream.resultChannel()) {
                    res.maybeFold(
                        onHeaders = { result ->
                            val responseHeaders = result.headers
                            assertThat(responseHeaders[leadingKey]).isEqualTo(listOf(leadingValue))
                            countDownLatch.countDown()
                        },
                        onMessage = { result ->
                            assertThat(result.message.payload!!.body!!.size()).isEqualTo(size)
                            countDownLatch.countDown()
                        },
                        onCompletion = { result ->
                            val responseTrailers = result.trailers
                            assertThat(responseTrailers[trailingKey]).isEqualTo(listOf(trailingValue.b64Encode()))
                            assertThat(result.error).isNull()
                            countDownLatch.countDown()
                        }
                    )
                }
            }
            stream.send(
                streamingOutputCallRequest {
                    responseParameters.add(responseParameters { this.size = size })
                }
            )
            countDownLatch.await(5, TimeUnit.SECONDS)
            job.cancel()
            assertThat(countDownLatch.count).isZero()
        }
    }

    override suspend fun statusCodeAndMessage() = register("status_code_and_message") {
        val message = simpleRequest {
            responseStatus = echoStatus {
                code = Code.UNKNOWN.value
                message = "test status message"
            }
        }
        val response = testServiceConnectClient.unaryCall(message)
        response.failure { errorResponse ->
            assertThat(errorResponse.error).isNotNull()
            assertThat(errorResponse.code).isEqualTo(Code.UNKNOWN)
            assertThat(errorResponse.error.message).isEqualTo("test status message")
        }
        response.success {
            fail<Unit>("unexpected success")
        }
        assertThat(response.code).isEqualTo(Code.UNKNOWN)
    }

    override suspend fun specialStatus() = register("special_status") {
        val statusMessage =
            "\\t\\ntest with whitespace\\r\\nand Unicode BMP ☺ and non-BMP \uD83D\uDE08\\t\\n"
        val response = testServiceConnectClient.unaryCall(
            simpleRequest {
                responseStatus = echoStatus {
                    code = 2
                    message = statusMessage
                }
            }
        )
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

    override suspend fun timeoutOnSleepingServer() = register("timeout_on_sleeping_server") {
        val countDownLatch = CountDownLatch(1)
        val client = TestServiceClient(shortTimeoutClient)
        val request = streamingOutputCallRequest {
            payload = payload {
                body = ByteString.copyFrom(ByteArray(271828))
            }
            responseParameters.add(
                responseParameters {
                    size = 31415
                    intervalUs = 50_000
                }
            )
        }
        val stream = client.streamingOutputCall()
        withContext(Dispatchers.IO) {
            val job = async {
                for (res in stream.resultChannel()) {
                    res.maybeFold(
                        onCompletion = { result ->
                            assertThat(result.error).isNotNull()
                            assertThat(result.connectError()!!.code).isEqualTo(Code.DEADLINE_EXCEEDED)
                            assertThat(result.code).isEqualTo(Code.DEADLINE_EXCEEDED)
                            countDownLatch.countDown()
                        }
                    )
                }
            }
            stream.send(request)
            countDownLatch.await(5, TimeUnit.SECONDS)
            job.cancel()
            assertThat(countDownLatch.count).isZero()
            stream.close()
        }
    }

    override suspend fun unimplementedMethod() = register("unimplemented_method") {
        val response = testServiceConnectClient.unimplementedCall(empty {})
        assertThat(response.code).isEqualTo(Code.UNIMPLEMENTED)
    }

    override suspend fun unimplementedServerStreamingMethod() = register("unimplemented_server_streaming_method") {
        val countDownLatch = CountDownLatch(1)
        val stream = testServiceConnectClient.unimplementedStreamingOutputCall()
        withContext(Dispatchers.IO) {
            val job = async {
                for (res in stream.resultChannel()) {
                    res.maybeFold(
                        onCompletion = { result ->
                            assertThat(result.code).isEqualTo(Code.UNIMPLEMENTED)
                            countDownLatch.countDown()
                        }
                    )
                }
            }
            stream.send(empty {})
            countDownLatch.await(5, TimeUnit.SECONDS)
            job.cancel()
            assertThat(countDownLatch.count).isZero()
            stream.close()
        }
    }

    override suspend fun unimplementedService() = register("unimplemented_service") {
        val response = unimplementedServiceClient.unimplementedCall(empty {})
        assertThat(response.code).isEqualTo(Code.UNIMPLEMENTED)
    }

    override suspend fun unimplementedServerStreamingService() = register("unimplemented_server_streaming_service") {
        val countDownLatch = CountDownLatch(1)
        val stream = unimplementedServiceClient.unimplementedStreamingOutputCall()
        withContext(Dispatchers.IO) {
            val job = async {
                for (res in stream.resultChannel()) {
                    res.maybeFold(
                        onCompletion = { result ->
                            assertThat(result.code).isEqualTo(Code.UNIMPLEMENTED)
                            countDownLatch.countDown()
                        }
                    )
                }
            }
            stream.send(empty {})
            countDownLatch.await(5, TimeUnit.SECONDS)
            job.cancel()
            assertThat(countDownLatch.count).isZero()
            stream.close()
        }
    }

    override suspend fun failUnary() = register("fail_unary") {
        val expectedErrorDetail = errorDetail {
            reason = "soirée 🎉"
            domain = "connect-crosstest"
        }
        val response = testServiceConnectClient.failUnaryCall(simpleRequest {})
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
        assertThat(response.code).isEqualTo(Code.RESOURCE_EXHAUSTED)
    }

    override suspend fun failServerStreaming() = register("fail_server_streaming") {
        val countDownLatch = CountDownLatch(1)
        val expectedErrorDetail = errorDetail {
            reason = "soirée 🎉"
            domain = "connect-crosstest"
        }
        val stream = testServiceConnectClient.failStreamingOutputCall()
        withContext(Dispatchers.IO) {
            val job = async {
                for (res in stream.resultChannel()) {
                    res.maybeFold(
                        onCompletion = { result ->
                            // For some reason we keep timing out on these calls and not actually getting a real response like with grpc?
                            assertThat(result.code).isEqualTo(Code.RESOURCE_EXHAUSTED)
                            assertThat(result.connectError()!!.code).isEqualTo(Code.RESOURCE_EXHAUSTED)
                            assertThat(result.connectError()!!.message).isEqualTo("soirée 🎉")
                            assertThat(result.connectError()!!.unpackedDetails(ErrorDetail::class)).containsExactly(
                                expectedErrorDetail
                            )
                            countDownLatch.countDown()
                        }
                    )
                }
            }
            val sizes = listOf(
                31415,
                9,
                2653,
                58979
            )
            val parameters = sizes.mapIndexed { index, value ->
                responseParameters {
                    size = value
                    intervalUs = index * 10
                }
            }

            stream.send(
                streamingOutputCallRequest {
                    responseParameters.addAll(parameters)
                }
            )
            countDownLatch.await(5, TimeUnit.SECONDS)
            job.cancel()
            assertThat(countDownLatch.count).isZero()
            stream.close()
        }
    }

    override suspend fun getUnary() = register("get_unary") {
        val size = 314159
        val message = simpleRequest {
            responseSize = size
            payload = payload {
                body = ByteString.copyFrom(ByteArray(size))
            }
        }
        val response = testServiceConnectClient.cacheableUnaryCall(message)
        response.failure {
            fail<Unit>("expected error to be null", it.error)
        }
        response.success { success ->
            assertThat(success.message.payload?.body?.toByteArray()?.size).isEqualTo(size)
        }
    }
}

internal fun ByteArray.b64Encode(): String {
    return this.toByteString().base64()
}

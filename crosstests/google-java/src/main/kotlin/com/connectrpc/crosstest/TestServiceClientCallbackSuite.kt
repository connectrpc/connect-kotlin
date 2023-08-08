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
import com.connectrpc.crosstest.ssl.UnaryCallbackTestSuite
import com.connectrpc.impl.ProtocolClient
import com.google.protobuf.ByteString
import com.grpc.testing.ErrorDetail
import com.grpc.testing.TestServiceClient
import com.grpc.testing.UnimplementedServiceClient
import com.grpc.testing.echoStatus
import com.grpc.testing.empty
import com.grpc.testing.errorDetail
import com.grpc.testing.payload
import com.grpc.testing.simpleRequest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.system.measureTimeMillis

class TestServiceClientCallbackSuite(
    client: ProtocolClient
) : UnaryCallbackTestSuite {
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

    override suspend fun largeUnary() = register("large_unary") {
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
        val countDownLatch = CountDownLatch(1)
        testServiceConnectClient.unaryCall(message, headers) { response ->
            assertThat(response.code).isEqualTo(Code.OK)
            assertThat(response.headers[leadingKey]).containsExactly(leadingValue)
            assertThat(response.trailers[trailingKey]).containsExactly(trailingValue.b64Encode())
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

    override suspend fun statusCodeAndMessage() = register("status_code_and_message") {
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

    override suspend fun specialStatus() = register("special_status") {
        val statusMessage =
            "\\t\\ntest with whitespace\\r\\nand Unicode BMP ☺ and non-BMP \uD83D\uDE08\\t\\n"
        val countDownLatch = CountDownLatch(1)
        testServiceConnectClient.unaryCall(
            simpleRequest {
                responseStatus = echoStatus {
                    code = 2
                    message = statusMessage
                }
            }
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

    override suspend fun unimplementedMethod() = register("unimplemented_method") {
        val countDownLatch = CountDownLatch(1)
        testServiceConnectClient.unimplementedCall(empty {}) { response ->
            assertThat(response.code).isEqualTo(Code.UNIMPLEMENTED)
            countDownLatch.countDown()
        }
        countDownLatch.await(500, TimeUnit.MILLISECONDS)
        assertThat(countDownLatch.count).isZero()
    }

    override suspend fun unimplementedService() = register("unimplemented_service") {
        val countDownLatch = CountDownLatch(1)
        unimplementedServiceClient.unimplementedCall(empty {}) { response ->
            assertThat(response.code).isEqualTo(Code.UNIMPLEMENTED)
            countDownLatch.countDown()
        }
        countDownLatch.await(500, TimeUnit.MILLISECONDS)
        assertThat(countDownLatch.count).isZero()
    }

    override suspend fun failUnary() = register("fail_unary") {
        val expectedErrorDetail = errorDetail {
            reason = "soirée 🎉"
            domain = "connect-crosstest"
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
}

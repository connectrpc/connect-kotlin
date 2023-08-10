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

package build.buf.connect.crosstest

import build.buf.connect.Code
import build.buf.connect.ProtocolClientConfig
import build.buf.connect.RequestCompression
import build.buf.connect.compression.GzipCompressionPool
import build.buf.connect.crosstest.ssl.sslContext
import build.buf.connect.extensions.GoogleJavaProtobufStrategy
import build.buf.connect.impl.ProtocolClient
import build.buf.connect.okhttp.ConnectOkHttpClient
import build.buf.connect.protocols.NetworkProtocol
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
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Protocol
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters
import java.time.Duration
import java.util.Base64
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit


@RunWith(Parameterized::class)
class CrossTest(
    private val protocol: NetworkProtocol
) {
    private lateinit var connectClient: ProtocolClient
    private lateinit var shortTimeoutConnectClient: ProtocolClient
    private lateinit var unimplementedServiceClient: UnimplementedServiceClient
    private lateinit var testServiceConnectClient: TestServiceClient
    companion object {
        @JvmStatic
        @Parameters(name = "protocol")
        fun data(): Iterable<NetworkProtocol> {
            return arrayListOf(
                NetworkProtocol.CONNECT,
                NetworkProtocol.GRPC
            )
        }
    }

    @Before
    fun before() {
        val port = 8081
        val host = "https://localhost:$port"
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
                    .build()
            ),
            ProtocolClientConfig(
                host = host,
                serializationStrategy = GoogleJavaProtobufStrategy(),
                networkProtocol = protocol,
                requestCompression = RequestCompression(10, GzipCompressionPool),
                compressionPools = listOf(GzipCompressionPool)
            )
        )
        connectClient = ProtocolClient(
            httpClient = ConnectOkHttpClient(client),
            ProtocolClientConfig(
                host = host,
                serializationStrategy = GoogleJavaProtobufStrategy(),
                networkProtocol = protocol,
                requestCompression = RequestCompression(10, GzipCompressionPool),
                compressionPools = listOf(GzipCompressionPool)
            )
        )
        testServiceConnectClient = TestServiceClient(connectClient)
        unimplementedServiceClient = UnimplementedServiceClient(connectClient)
    }

    @Test
    fun failServerStreaming() = runBlocking {
        val countDownLatch = CountDownLatch(1)
        val expectedErrorDetail = errorDetail {
            reason = "soirée 🎉"
            domain = "connect-crosstest"
        }
        val stream = testServiceConnectClient.failStreamingOutputCall()
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
            countDownLatch.await(5, TimeUnit.SECONDS)
            job.cancel()
            assertThat(countDownLatch.count).isZero()
            stream.close()
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
                trailingKey to listOf(b64Encode(trailingValue))
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
    fun timeoutOnSleepingServer() = runBlocking {
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
    fun failUnary(): Unit = runBlocking {
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
                trailingKey to listOf(b64Encode(trailingValue))
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

//    @Test
//    fun unimplementedServiceCallback(): Unit = runBlocking {
//        val countDownLatch = CountDownLatch(1)
//        unimplementedServiceClient.unimplementedCall(empty {}) { response ->
//            assertThat(response.code).isEqualTo(Code.UNIMPLEMENTED)
//            countDownLatch.countDown()
//        }
//        countDownLatch.await(500, TimeUnit.MILLISECONDS)
//        assertThat(countDownLatch.count).isZero()
//    }

//    @Test
//    fun failUnaryCallback(): Unit = runBlocking {
//        val expectedErrorDetail = errorDetail {
//            reason = "soirée 🎉"
//            domain = "connect-crosstest"
//        }
//        val countDownLatch = CountDownLatch(1)
//        testServiceConnectClient.failUnaryCall(simpleRequest {}) { response ->
//            assertThat(response.code).isEqualTo(Code.RESOURCE_EXHAUSTED)
//            response.failure { errorResponse ->
//                val error = errorResponse.error
//                assertThat(error.code).isEqualTo(Code.RESOURCE_EXHAUSTED)
//                assertThat(error.message).isEqualTo("soirée 🎉")
//                val connectErrorDetails = error.unpackedDetails(ErrorDetail::class)
//                assertThat(connectErrorDetails).containsExactly(expectedErrorDetail)
//                countDownLatch.countDown()
//            }
//            response.success {
//                fail<Unit>("unexpected success")
//            }
//        }
//        countDownLatch.await(500, TimeUnit.MILLISECONDS)
//        assertThat(countDownLatch.count).isZero()
//    }

    private fun b64Encode(trailingValue: ByteArray): String {
        return String(Base64.getEncoder().encode(trailingValue))
    }
}

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

import build.buf.connect.ProtocolClientConfig
import build.buf.connect.compression.GzipCompressionPool
import build.buf.connect.compression.RequestCompression
import build.buf.connect.crosstest.ssl.sslContext
import build.buf.connect.extensions.GoogleJavaProtobufStrategy
import build.buf.connect.impl.ProtocolClient
import build.buf.connect.okhttp.ConnectOkHttpClient
import build.buf.connect.protocols.NetworkProtocol
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Protocol
import java.time.Duration
import kotlin.system.exitProcess

class Main {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            runBlocking {
                val port = 8081
                val host = "https://localhost:$port"
                println("Starting on $host...")
                val (sslSocketFactory, trustManager) = sslContext()
                val client = OkHttpClient.Builder()
                    .protocols(listOf(Protocol.HTTP_2, Protocol.HTTP_1_1))
                    .connectTimeout(Duration.ofMinutes(1))
                    .readTimeout(Duration.ofMinutes(1))
                    .writeTimeout(Duration.ofMinutes(1))
                    .callTimeout(Duration.ofMinutes(1))
                    .sslSocketFactory(sslSocketFactory, trustManager)
                    .build()
                val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
                val job = scope.launch {
                    runConnectTests("connect", host, client, NetworkProtocol.CONNECT)
                    runConnectTests("grpc", host, client, NetworkProtocol.GRPC)
                    runConnectTests("grpc-web", host, client, NetworkProtocol.GRPC_WEB)
                }
                job.join()
                println("...complete.")
                exitProcess(0)
            }
        }

        private suspend fun runConnectTests(
            tag: String,
            host: String,
            client: OkHttpClient,
            networkProtocol: NetworkProtocol
        ) {
            val connectClient = ProtocolClient(
                httpClient = ConnectOkHttpClient(client),
                ProtocolClientConfig(
                    host = host,
                    serializationStrategy = GoogleJavaProtobufStrategy(),
                    networkProtocol = networkProtocol,
                    requestCompression = RequestCompression(10, GzipCompressionPool),
                    compressionPools = listOf(GzipCompressionPool)
                )
            )
            val shortTimeoutClient = ProtocolClient(
                httpClient = ConnectOkHttpClient(client),
                ProtocolClientConfig(
                    host = host,
                    serializationStrategy = GoogleJavaProtobufStrategy(),
                    networkProtocol = networkProtocol,
                    requestCompression = RequestCompression(10, GzipCompressionPool),
                    compressionPools = listOf(GzipCompressionPool)
                )
            )
            coroutineTests(tag, connectClient, shortTimeoutClient)
            callbackTests(tag, connectClient)
        }

        private suspend fun coroutineTests(
            tag: String,
            protocolClient: ProtocolClient,
            shortTimeoutClient: ProtocolClient
        ) {
            val testServiceClientSuite = TestServiceClientSuite(protocolClient, shortTimeoutClient)
//            testServiceClientSuite.emptyUnary()
//            testServiceClientSuite.largeUnary()
//            testServiceClientSuite.serverStreaming()
//            testServiceClientSuite.emptyStream()
//            testServiceClientSuite.customMetadata()
//            testServiceClientSuite.customMetadataServerStreaming()
//            testServiceClientSuite.statusCodeAndMessage()
//            testServiceClientSuite.specialStatus()
//            testServiceClientSuite.timeoutOnSleepingServer()
//            testServiceClientSuite.unimplementedMethod()
//            testServiceClientSuite.unimplementedServerStreamingMethod()
//            testServiceClientSuite.unimplementedService()
//            testServiceClientSuite.unimplementedServerStreamingService()
//            testServiceClientSuite.failUnary()
//            testServiceClientSuite.failServerStreaming()
            testServiceClientSuite.getUnary()

            testServiceClientSuite.test(tag)
        }

        private suspend fun callbackTests(
            tag: String,
            protocolClient: ProtocolClient
        ) {
            val testServiceClientSuite = TestServiceClientCallbackSuite(protocolClient)
//            testServiceClientSuite.emptyUnary()
//            testServiceClientSuite.largeUnary()
//            testServiceClientSuite.customMetadata()
//            testServiceClientSuite.statusCodeAndMessage()
//            testServiceClientSuite.specialStatus()
//            testServiceClientSuite.unimplementedMethod()
//            testServiceClientSuite.unimplementedService()
//            testServiceClientSuite.failUnary()

            testServiceClientSuite.test(tag)
        }
    }
}

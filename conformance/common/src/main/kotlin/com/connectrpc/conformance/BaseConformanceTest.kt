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

import com.connectrpc.ProtocolClientConfig
import com.connectrpc.RequestCompression
import com.connectrpc.SerializationStrategy
import com.connectrpc.compression.GzipCompressionPool
import com.connectrpc.conformance.ssl.sslContext
import com.connectrpc.impl.ProtocolClient
import com.connectrpc.okhttp.ConnectOkHttpClient
import com.connectrpc.protocols.NetworkProtocol
import okhttp3.OkHttpClient
import okhttp3.Protocol
import org.junit.ClassRule
import org.junit.runners.Parameterized
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy
import java.time.Duration
import java.util.Base64

abstract class BaseConformanceTest(
    private val protocol: NetworkProtocol,
    private val serverType: ServerType,
) {
    lateinit var connectClient: ProtocolClient
    lateinit var shortTimeoutConnectClient: ProtocolClient

    companion object {
        private const val CONFORMANCE_VERSION = "88f85130640b46c0837e0d58c0484d83a110f418"

        @JvmStatic
        @Parameterized.Parameters(name = "client={0},server={1}")
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

    fun init(serializationStrategy: SerializationStrategy) {
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
                serializationStrategy = serializationStrategy,
                networkProtocol = protocol,
                requestCompression = RequestCompression(10, GzipCompressionPool),
                compressionPools = listOf(GzipCompressionPool),
            ),
        )
        connectClient = ProtocolClient(
            httpClient = ConnectOkHttpClient(client),
            ProtocolClientConfig(
                host = host,
                serializationStrategy = serializationStrategy,
                networkProtocol = protocol,
                requestCompression = RequestCompression(10, GzipCompressionPool),
                compressionPools = listOf(GzipCompressionPool),
            ),
        )
    }

    fun b64Encode(trailingValue: ByteArray): String {
        return String(Base64.getEncoder().encode(trailingValue))
    }
}

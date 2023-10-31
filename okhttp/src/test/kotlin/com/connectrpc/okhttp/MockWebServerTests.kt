package com.connectrpc.okhttp

import com.connectrpc.ProtocolClientConfig
import com.connectrpc.RequestCompression
import com.connectrpc.compression.GzipCompressionPool
import com.connectrpc.conformance.v1.TestServiceClient
import com.connectrpc.conformance.v1.simpleRequest
import com.connectrpc.extensions.GoogleJavaProtobufStrategy
import com.connectrpc.impl.ProtocolClient
import com.connectrpc.protocols.NetworkProtocol
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class MockWebServerTests {

    @Test
    fun `compressed empty failure response is parsed correctly`() = runTest {
        val mockWebServer = MockWebServer()
        mockWebServer.start()

        mockWebServer.enqueue(
            MockResponse().apply {
                addHeader("accept-encoding", "gzip")
                addHeader("content-encoding", "gzip")
                setBody("{}")
                setResponseCode(401)
            },
        )

        val host = mockWebServer.url("/")

        val protocolClient = ProtocolClient(
            ConnectOkHttpClient(
                OkHttpClient.Builder()
                    .protocols(listOf(Protocol.HTTP_2, Protocol.HTTP_1_1))
                    .build()
            ),
            ProtocolClientConfig(
                host = host.toString(),
                serializationStrategy = GoogleJavaProtobufStrategy(),
                networkProtocol = NetworkProtocol.CONNECT,
                requestCompression = RequestCompression(0, GzipCompressionPool),
                compressionPools = listOf(GzipCompressionPool),
            ),
        )

        val request = simpleRequest {}
        TestServiceClient(protocolClient).unaryCall(request)

        mockWebServer.takeRequest().apply {
            assertThat(path).isEqualTo("/connectrpc.conformance.v1.TestService/UnaryCall")
        }

        mockWebServer.shutdown()
    }
}
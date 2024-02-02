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

package com.connectrpc.okhttp

import com.connectrpc.Code
import com.connectrpc.ProtocolClientConfig
import com.connectrpc.RequestCompression
import com.connectrpc.ResponseMessage
import com.connectrpc.SerializationStrategy
import com.connectrpc.compression.GzipCompressionPool
import com.connectrpc.eliza.v1.ElizaServiceClient
import com.connectrpc.eliza.v1.sayRequest
import com.connectrpc.extensions.GoogleJavaJSONStrategy
import com.connectrpc.extensions.GoogleJavaProtobufStrategy
import com.connectrpc.impl.ProtocolClient
import com.connectrpc.protocols.NetworkProtocol
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.mockwebserver.MockResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test

/**
 * Tests to exercise end to end failure cases not easily verified with conformance tests.
 * Over time these may be moved to conformance tests.
 */
class MockWebServerTests {

    @get:Rule val mockWebServerRule = MockWebServerRule()

    @Test
    fun `invalid compressed failure response is handled correctly`() = runTest {
        mockWebServerRule.server.enqueue(
            MockResponse().apply {
                addHeader("accept-encoding", "gzip")
                addHeader("content-encoding", "gzip")
                setBody("{}")
                setResponseCode(401)
            },
        )
        val response = createClient().say(sayRequest { sentence = "hello" })
        mockWebServerRule.server.takeRequest().apply {
            assertThat(path).isEqualTo("/connectrpc.eliza.v1.ElizaService/Say")
        }
        assertThat(response).isInstanceOf(ResponseMessage::class.java)
        response.failure { assertThat(it.cause.code).isEqualTo(Code.INTERNAL_ERROR) }
    }

    @Test
    fun `invalid compressed response data is handled correctly`() = runTest {
        mockWebServerRule.server.enqueue(
            MockResponse().apply {
                addHeader("accept-encoding", "gzip")
                addHeader("content-encoding", "gzip")
                setBody("this isn't gzipped")
                setResponseCode(200)
            },
        )
        val response = createClient().say(sayRequest { sentence = "hello" })
        mockWebServerRule.server.takeRequest().apply {
            assertThat(path).isEqualTo("/connectrpc.eliza.v1.ElizaService/Say")
        }
        assertThat(response).isInstanceOf(ResponseMessage::class.java)
        response.failure { assertThat(it.cause.code).isEqualTo(Code.INTERNAL_ERROR) }
    }

    @Test
    fun `invalid protobuf response data is handled correctly`() = runTest {
        mockWebServerRule.server.enqueue(
            MockResponse().apply {
                addHeader("accept-encoding", "gzip")
                addHeader("content-type", "application/proto")
                setBody("this isn't valid protobuf")
                setResponseCode(200)
            },
        )
        val response = createClient().say(sayRequest { sentence = "hello" })
        mockWebServerRule.server.takeRequest().apply {
            assertThat(path).isEqualTo("/connectrpc.eliza.v1.ElizaService/Say")
        }
        assertThat(response).isInstanceOf(ResponseMessage::class.java)
        response.failure { assertThat(it.cause.code).isEqualTo(Code.INTERNAL_ERROR) }
    }

    @Test
    fun `invalid json response data is handled correctly`() = runTest {
        mockWebServerRule.server.enqueue(
            MockResponse().apply {
                addHeader("accept-encoding", "gzip")
                addHeader("content-type", "application/json")
                setBody("{ invalid json")
                setResponseCode(200)
            },
        )
        val response = createClient(serializationStrategy = GoogleJavaJSONStrategy()).say(sayRequest { sentence = "hello" })
        mockWebServerRule.server.takeRequest().apply {
            assertThat(path).isEqualTo("/connectrpc.eliza.v1.ElizaService/Say")
        }
        assertThat(response).isInstanceOf(ResponseMessage::class.java)
        response.failure { assertThat(it.cause.code).isEqualTo(Code.INTERNAL_ERROR) }
    }

    private fun createClient(serializationStrategy: SerializationStrategy = GoogleJavaProtobufStrategy()): ElizaServiceClient {
        val host = mockWebServerRule.server.url("/")
        val protocolClient = ProtocolClient(
            ConnectOkHttpClient(
                OkHttpClient.Builder()
                    .protocols(listOf(Protocol.HTTP_2, Protocol.HTTP_1_1))
                    .build(),
            ),
            ProtocolClientConfig(
                host = host.toString(),
                serializationStrategy = serializationStrategy,
                networkProtocol = NetworkProtocol.CONNECT,
                requestCompression = RequestCompression(0, GzipCompressionPool),
            ),
        )
        return ElizaServiceClient(protocolClient)
    }
}

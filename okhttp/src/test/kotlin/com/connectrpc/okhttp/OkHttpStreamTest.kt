// Copyright 2022-2025 The Connect Authors
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
import com.connectrpc.ConnectException
import com.connectrpc.NetworkProtocol
import com.connectrpc.ProtocolClientConfig
import com.connectrpc.eliza.v1.ElizaServiceClient
import com.connectrpc.extensions.GoogleJavaProtobufStrategy
import com.connectrpc.impl.ProtocolClient
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import mockwebserver3.MockResponse
import mockwebserver3.junit4.MockWebServerRule
import okhttp3.OkHttpClient
import okhttp3.Protocol
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

/**
 * Tests for OkHttp stream handling, specifically connection lifecycle.
 */
class OkHttpStreamTest {

    @get:Rule val mockWebServerRule = MockWebServerRule()

    // Regression test for https://github.com/connectrpc/connect-kotlin/issues/395
    // A non-200 streaming response must close the OkHttp response body.
    // Before the fix, the response body was not closed on non-200 responses,
    // causing OkHttp's connection leak detector to fire.
    @Test
    fun `non-200 streaming response completes and closes response body`() {
        mockWebServerRule.server.enqueue(
            MockResponse.Builder().apply {
                code(503)
                addHeader("content-type", "application/connect+proto")
            }.build(),
        )
        val client = createStreamingClient()
        runBlocking {
            val stream = client.converse()
            val exception = withTimeout(10.seconds) {
                try {
                    for (msg in stream.responseChannel()) {
                        // Should not receive any messages.
                    }
                    null
                } catch (e: ConnectException) {
                    e
                }
            }
            assertThat(exception).isNotNull()
            // The exact code depends on protocol interceptor processing,
            // but the key assertion is that the stream completes at all
            // (before the fix, the response body was never closed on
            // non-200 responses, causing a connection leak).
            assertThat(exception!!.code).isIn(Code.UNAVAILABLE, Code.UNKNOWN, Code.INTERNAL)
            stream.receiveClose()
        }
    }

    // Verify that a 200 streaming response with an empty body also
    // completes cleanly.
    @Test
    fun `empty streaming response completes and closes response body`() {
        mockWebServerRule.server.enqueue(
            MockResponse.Builder().apply {
                code(200)
                addHeader("content-type", "application/connect+proto")
            }.build(),
        )
        val client = createStreamingClient()
        runBlocking {
            val stream = client.converse()
            withTimeout(10.seconds) {
                try {
                    for (msg in stream.responseChannel()) {
                        // drain
                    }
                } catch (_: ConnectException) {
                    // May get an error from the protocol interceptor due to
                    // unexpected content-type or missing end-of-stream.
                }
            }
            stream.receiveClose()
        }
    }

    private fun createStreamingClient(): ElizaServiceClient {
        val host = mockWebServerRule.server.url("/")
        val okHttpClient = OkHttpClient.Builder()
            .protocols(listOf(Protocol.HTTP_2, Protocol.HTTP_1_1))
            .build()
        val protocolClient = ProtocolClient(
            ConnectOkHttpClient(okHttpClient),
            ProtocolClientConfig(
                host = host.toString(),
                serializationStrategy = GoogleJavaProtobufStrategy(),
                networkProtocol = NetworkProtocol.CONNECT,
                timeoutOracle = { null },
            ),
        )
        return ElizaServiceClient(protocolClient)
    }
}

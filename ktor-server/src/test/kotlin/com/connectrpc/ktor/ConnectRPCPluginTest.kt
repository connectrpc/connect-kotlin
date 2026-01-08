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

package com.connectrpc.ktor

import com.connectrpc.StreamType
import com.connectrpc.server.HandlerSpec
import com.connectrpc.server.UnaryHandler
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.application.install
import io.ktor.server.testing.testApplication
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ConnectRPCPluginTest {

    @Test
    fun `handles unary RPC request`() = testApplication {
        application {
            install(ConnectRPC) {
                serializationStrategy = KtorTestSerializationStrategy()
                handler(
                    HandlerSpec(
                        procedure = "test.Service/Echo",
                        requestClass = String::class,
                        responseClass = String::class,
                        streamType = StreamType.UNARY,
                        handler = UnaryHandler { _, req -> "Hello, $req!" },
                    ),
                )
            }
        }

        val response = client.post("/test.Service/Echo") {
            contentType(ContentType.Application.Json)
            setBody("World")
        }

        assertThat(response.status).isEqualTo(HttpStatusCode.OK)
        assertThat(response.bodyAsText()).isEqualTo("Hello, World!")
    }

    @Test
    fun `returns 404 for unknown procedure`() = testApplication {
        application {
            install(ConnectRPC) {
                serializationStrategy = KtorTestSerializationStrategy()
            }
        }

        val response = client.post("/unknown.Service/Method") {
            contentType(ContentType.Application.Json)
            setBody("{}")
        }

        // When handler is not found, the plugin doesn't handle the call
        // Ktor returns 404 by default for unhandled routes
        assertThat(response.status).isEqualTo(HttpStatusCode.NotFound)
    }

    @Test
    fun `registers multiple handlers`() = testApplication {
        application {
            install(ConnectRPC) {
                serializationStrategy = KtorTestSerializationStrategy()
                handler(
                    HandlerSpec(
                        procedure = "test.Service/Method1",
                        requestClass = String::class,
                        responseClass = String::class,
                        streamType = StreamType.UNARY,
                        handler = UnaryHandler { _, _ -> "response1" },
                    ),
                )
                handler(
                    HandlerSpec(
                        procedure = "test.Service/Method2",
                        requestClass = String::class,
                        responseClass = String::class,
                        streamType = StreamType.UNARY,
                        handler = UnaryHandler { _, _ -> "response2" },
                    ),
                )
            }
        }

        val response1 = client.post("/test.Service/Method1") {
            contentType(ContentType.Application.Json)
            setBody("")
        }
        assertThat(response1.bodyAsText()).isEqualTo("response1")

        val response2 = client.post("/test.Service/Method2") {
            contentType(ContentType.Application.Json)
            setBody("")
        }
        assertThat(response2.bodyAsText()).isEqualTo("response2")
    }

    @Test
    fun `sets response headers from context`() = testApplication {
        application {
            install(ConnectRPC) {
                serializationStrategy = KtorTestSerializationStrategy()
                handler(
                    HandlerSpec(
                        procedure = "test.Service/WithHeaders",
                        requestClass = String::class,
                        responseClass = String::class,
                        streamType = StreamType.UNARY,
                        handler = UnaryHandler { ctx, req ->
                            ctx.setResponseHeader("x-custom-header", "custom-value")
                            req
                        },
                    ),
                )
            }
        }

        val response = client.post("/test.Service/WithHeaders") {
            contentType(ContentType.Application.Json)
            setBody("test")
        }

        assertThat(response.status).isEqualTo(HttpStatusCode.OK)
        assertThat(response.headers["x-custom-header"]).isEqualTo("custom-value")
    }
}

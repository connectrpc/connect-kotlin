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
import com.connectrpc.compression.GzipCompressionPool
import com.connectrpc.server.HandlerSpec
import com.connectrpc.server.ServerConfig
import com.connectrpc.server.ServiceHandler
import com.connectrpc.server.UnaryHandler
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class RoutingTest {

    @Test
    fun `connectRPC DSL registers handlers`() = testApplication {
        application {
            routing {
                connectRPC(
                    ServerConfig(
                        serializationStrategy = KtorTestSerializationStrategy(),
                        compressionPools = listOf(GzipCompressionPool),
                    ),
                ) {
                    handler(
                        HandlerSpec(
                            procedure = "test.Service/Method",
                            requestClass = String::class,
                            responseClass = String::class,
                            streamType = StreamType.UNARY,
                            handler = UnaryHandler { _, req -> "Received: $req" },
                        ),
                    )
                }
            }
        }

        val response = client.post("/test.Service/Method") {
            contentType(ContentType.Application.Json)
            setBody("hello")
        }

        assertThat(response.status).isEqualTo(HttpStatusCode.OK)
        assertThat(response.bodyAsText()).isEqualTo("Received: hello")
    }

    @Test
    fun `mountConnectRPC registers handlers at path prefix`() = testApplication {
        application {
            routing {
                mountConnectRPC(
                    pathPrefix = "/api",
                    config = ServerConfig(
                        serializationStrategy = KtorTestSerializationStrategy(),
                        compressionPools = listOf(GzipCompressionPool),
                    ),
                ) {
                    handler(
                        HandlerSpec(
                            procedure = "test.Service/Method",
                            requestClass = String::class,
                            responseClass = String::class,
                            streamType = StreamType.UNARY,
                            handler = UnaryHandler { _, req -> "API: $req" },
                        ),
                    )
                }
            }
        }

        val response = client.post("/api/test.Service/Method") {
            contentType(ContentType.Application.Json)
            setBody("data")
        }

        assertThat(response.status).isEqualTo(HttpStatusCode.OK)
        assertThat(response.bodyAsText()).isEqualTo("API: data")
    }

    @Test
    fun `service DSL registers handlers from service handler`() = testApplication {
        val testServiceHandler = object : ServiceHandler {
            override val serviceName: String = "test.TestService"
        }

        application {
            routing {
                connectRPC(
                    ServerConfig(
                        serializationStrategy = KtorTestSerializationStrategy(),
                        compressionPools = listOf(GzipCompressionPool),
                    ),
                ) {
                    service(testServiceHandler) { _ ->
                        listOf(
                            HandlerSpec(
                                procedure = "test.TestService/Method1",
                                requestClass = String::class,
                                responseClass = String::class,
                                streamType = StreamType.UNARY,
                                handler = UnaryHandler { _, _ -> "method1" },
                            ),
                            HandlerSpec(
                                procedure = "test.TestService/Method2",
                                requestClass = String::class,
                                responseClass = String::class,
                                streamType = StreamType.UNARY,
                                handler = UnaryHandler { _, _ -> "method2" },
                            ),
                        )
                    }
                }
            }
        }

        val response1 = client.post("/test.TestService/Method1") {
            contentType(ContentType.Application.Json)
            setBody("")
        }
        assertThat(response1.bodyAsText()).isEqualTo("method1")

        val response2 = client.post("/test.TestService/Method2") {
            contentType(ContentType.Application.Json)
            setBody("")
        }
        assertThat(response2.bodyAsText()).isEqualTo("method2")
    }
}

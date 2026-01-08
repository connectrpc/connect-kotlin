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

package com.connectrpc.server

import com.connectrpc.StreamType
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ConnectServerTest {

    @Test
    fun `registerHandlers adds handlers`() {
        val config = ServerConfig(
            serializationStrategy = TestSerializationStrategy(),
        )
        val server = ConnectServer(config)

        val spec = HandlerSpec(
            procedure = "test.Service/Method",
            requestClass = String::class,
            responseClass = String::class,
            streamType = StreamType.UNARY,
            handler = UnaryHandler { _, req -> req },
        )

        server.registerHandlers(spec)

        assertThat(server.findHandler("test.Service/Method")).isEqualTo(spec)
    }

    @Test
    fun `registerHandlers adds multiple handlers`() {
        val config = ServerConfig(
            serializationStrategy = TestSerializationStrategy(),
        )
        val server = ConnectServer(config)

        val spec1 = HandlerSpec(
            procedure = "test.Service/Method1",
            requestClass = String::class,
            responseClass = String::class,
            streamType = StreamType.UNARY,
            handler = UnaryHandler { _, req -> req },
        )
        val spec2 = HandlerSpec(
            procedure = "test.Service/Method2",
            requestClass = String::class,
            responseClass = String::class,
            streamType = StreamType.UNARY,
            handler = UnaryHandler { _, req -> req },
        )

        server.registerHandlers(spec1, spec2)

        assertThat(server.findHandler("test.Service/Method1")).isEqualTo(spec1)
        assertThat(server.findHandler("test.Service/Method2")).isEqualTo(spec2)
    }

    @Test
    fun `findHandler returns null for unknown procedure`() {
        val config = ServerConfig(
            serializationStrategy = TestSerializationStrategy(),
        )
        val server = ConnectServer(config)

        assertThat(server.findHandler("unknown.Service/Method")).isNull()
    }

    @Test
    fun `registeredProcedures returns all registered procedures`() {
        val config = ServerConfig(
            serializationStrategy = TestSerializationStrategy(),
        )
        val server = ConnectServer(config)

        val spec1 = HandlerSpec(
            procedure = "test.Service/Method1",
            requestClass = String::class,
            responseClass = String::class,
            streamType = StreamType.UNARY,
            handler = UnaryHandler { _, req -> req },
        )
        val spec2 = HandlerSpec(
            procedure = "test.Service/Method2",
            requestClass = String::class,
            responseClass = String::class,
            streamType = StreamType.UNARY,
            handler = UnaryHandler { _, req -> req },
        )

        server.registerHandlers(spec1, spec2)

        assertThat(server.registeredProcedures()).containsExactlyInAnyOrder(
            "test.Service/Method1",
            "test.Service/Method2",
        )
    }

    @Test
    fun `registeredProcedures is empty initially`() {
        val config = ServerConfig(
            serializationStrategy = TestSerializationStrategy(),
        )
        val server = ConnectServer(config)

        assertThat(server.registeredProcedures()).isEmpty()
    }

    @Test
    fun `registerService registers all handlers from service`() {
        val config = ServerConfig(
            serializationStrategy = TestSerializationStrategy(),
        )
        val server = ConnectServer(config)

        val serviceHandler = object : ServiceHandler {
            override val serviceName: String = "test.Service"
        }

        server.registerService(serviceHandler) { _ ->
            listOf(
                HandlerSpec(
                    procedure = "test.Service/Method1",
                    requestClass = String::class,
                    responseClass = String::class,
                    streamType = StreamType.UNARY,
                    handler = UnaryHandler { _, req -> req },
                ),
                HandlerSpec(
                    procedure = "test.Service/Method2",
                    requestClass = String::class,
                    responseClass = String::class,
                    streamType = StreamType.UNARY,
                    handler = UnaryHandler { _, req -> req },
                ),
            )
        }

        assertThat(server.registeredProcedures()).containsExactlyInAnyOrder(
            "test.Service/Method1",
            "test.Service/Method2",
        )
    }
}

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

class HandlerSpecTest {

    @Test
    fun `extracts service name from procedure`() {
        val spec = HandlerSpec(
            procedure = "connectrpc.eliza.v1.ElizaService/Say",
            requestClass = String::class,
            responseClass = String::class,
            streamType = StreamType.UNARY,
            handler = UnaryHandler { _, req -> req },
        )

        assertThat(spec.serviceName).isEqualTo("connectrpc.eliza.v1.ElizaService")
    }

    @Test
    fun `extracts method name from procedure`() {
        val spec = HandlerSpec(
            procedure = "connectrpc.eliza.v1.ElizaService/Say",
            requestClass = String::class,
            responseClass = String::class,
            streamType = StreamType.UNARY,
            handler = UnaryHandler { _, req -> req },
        )

        assertThat(spec.methodName).isEqualTo("Say")
    }

    @Test
    fun `handles simple procedure path`() {
        val spec = HandlerSpec(
            procedure = "Service/Method",
            requestClass = String::class,
            responseClass = String::class,
            streamType = StreamType.UNARY,
            handler = UnaryHandler { _, req -> req },
        )

        assertThat(spec.serviceName).isEqualTo("Service")
        assertThat(spec.methodName).isEqualTo("Method")
    }
}

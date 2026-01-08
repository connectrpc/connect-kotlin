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

import kotlinx.coroutines.Dispatchers
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class ServerContextTest {

    @Test
    fun `provides access to request headers`() {
        val headers = mapOf("x-custom-header" to listOf("value1", "value2"))
        val context = ServerContextImpl(
            requestHeaders = headers,
            procedure = "test.Service/Method",
            timeout = null,
            coroutineContext = Dispatchers.Default,
        )

        assertThat(context.requestHeaders).isEqualTo(headers)
    }

    @Test
    fun `provides access to procedure`() {
        val context = ServerContextImpl(
            requestHeaders = emptyMap(),
            procedure = "connectrpc.eliza.v1.ElizaService/Say",
            timeout = null,
            coroutineContext = Dispatchers.Default,
        )

        assertThat(context.procedure).isEqualTo("connectrpc.eliza.v1.ElizaService/Say")
    }

    @Test
    fun `provides access to timeout`() {
        val context = ServerContextImpl(
            requestHeaders = emptyMap(),
            procedure = "test.Service/Method",
            timeout = 30.seconds,
            coroutineContext = Dispatchers.Default,
        )

        assertThat(context.timeout).isEqualTo(30.seconds)
    }

    @Test
    fun `timeout is null when not specified`() {
        val context = ServerContextImpl(
            requestHeaders = emptyMap(),
            procedure = "test.Service/Method",
            timeout = null,
            coroutineContext = Dispatchers.Default,
        )

        assertThat(context.timeout).isNull()
    }

    @Test
    fun `can set and get response headers`() {
        val context = ServerContextImpl(
            requestHeaders = emptyMap(),
            procedure = "test.Service/Method",
            timeout = null,
            coroutineContext = Dispatchers.Default,
        )

        context.setResponseHeader("x-response-header", "value1")
        context.setResponseHeader("x-another-header", "value2")

        assertThat(context.responseHeaders["x-response-header"]).containsExactly("value1")
        assertThat(context.responseHeaders["x-another-header"]).containsExactly("value2")
    }

    @Test
    fun `can add multiple values to same response header`() {
        val context = ServerContextImpl(
            requestHeaders = emptyMap(),
            procedure = "test.Service/Method",
            timeout = null,
            coroutineContext = Dispatchers.Default,
        )

        context.setResponseHeader("x-multi-value", "value1")
        context.setResponseHeader("x-multi-value", "value2")

        assertThat(context.responseHeaders["x-multi-value"]).containsExactly("value1", "value2")
    }

    @Test
    fun `can set and get response trailers`() {
        val context = ServerContextImpl(
            requestHeaders = emptyMap(),
            procedure = "test.Service/Method",
            timeout = null,
            coroutineContext = Dispatchers.Default,
        )

        context.setResponseTrailer("x-response-trailer", "trailer-value")

        assertThat(context.responseTrailers["x-response-trailer"]).containsExactly("trailer-value")
    }

    @Test
    fun `can add multiple values to same response trailer`() {
        val context = ServerContextImpl(
            requestHeaders = emptyMap(),
            procedure = "test.Service/Method",
            timeout = null,
            coroutineContext = Dispatchers.Default,
        )

        context.setResponseTrailer("x-multi-trailer", "trailer1")
        context.setResponseTrailer("x-multi-trailer", "trailer2")

        assertThat(context.responseTrailers["x-multi-trailer"]).containsExactly("trailer1", "trailer2")
    }

    @Test
    fun `response headers and trailers are initially empty`() {
        val context = ServerContextImpl(
            requestHeaders = emptyMap(),
            procedure = "test.Service/Method",
            timeout = null,
            coroutineContext = Dispatchers.Default,
        )

        assertThat(context.responseHeaders).isEmpty()
        assertThat(context.responseTrailers).isEmpty()
    }
}

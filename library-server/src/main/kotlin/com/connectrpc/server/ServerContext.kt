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

import com.connectrpc.Headers
import com.connectrpc.Trailers
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration

/**
 * Context passed to RPC handlers.
 *
 * Provides access to request metadata and allows setting response metadata.
 */
interface ServerContext {
    /**
     * The request headers.
     */
    val requestHeaders: Headers

    /**
     * The procedure being called (e.g., "connectrpc.eliza.v1.ElizaService/Say").
     */
    val procedure: String

    /**
     * The timeout for this RPC, if specified by the client.
     * Null means no timeout was specified.
     */
    val timeout: Duration?

    /**
     * Sets a response header.
     *
     * Must be called before the response is sent.
     *
     * @param key The header name.
     * @param value The header value.
     */
    fun setResponseHeader(key: String, value: String)

    /**
     * Sets a response trailer.
     *
     * @param key The trailer name.
     * @param value The trailer value.
     */
    fun setResponseTrailer(key: String, value: String)

    /**
     * Gets the current response headers.
     */
    val responseHeaders: Headers

    /**
     * Gets the current response trailers.
     */
    val responseTrailers: Trailers

    /**
     * The coroutine context for this RPC.
     *
     * Can be used to check for cancellation or access coroutine-scoped values.
     */
    val coroutineContext: CoroutineContext
}

/**
 * Default implementation of [ServerContext].
 */
internal class ServerContextImpl(
    override val requestHeaders: Headers,
    override val procedure: String,
    override val timeout: Duration?,
    override val coroutineContext: CoroutineContext,
) : ServerContext {
    private val _responseHeaders = mutableMapOf<String, MutableList<String>>()
    private val _responseTrailers = mutableMapOf<String, MutableList<String>>()

    override val responseHeaders: Headers
        get() = _responseHeaders.mapValues { it.value.toList() }

    override val responseTrailers: Trailers
        get() = _responseTrailers.mapValues { it.value.toList() }

    override fun setResponseHeader(key: String, value: String) {
        _responseHeaders.getOrPut(key) { mutableListOf() }.add(value)
    }

    override fun setResponseTrailer(key: String, value: String) {
        _responseTrailers.getOrPut(key) { mutableListOf() }.add(value)
    }
}

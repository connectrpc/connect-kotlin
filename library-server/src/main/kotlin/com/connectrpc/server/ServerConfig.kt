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

import com.connectrpc.SerializationStrategy
import com.connectrpc.compression.CompressionPool
import com.connectrpc.compression.GzipCompressionPool
import kotlin.coroutines.CoroutineContext

/**
 * Configuration for the Connect server.
 *
 * Unlike the client configuration, the server supports all three protocols
 * (Connect, gRPC, gRPC-Web) simultaneously by default.
 */
class ServerConfig(
    /**
     * The serialization strategy for encoding/decoding messages.
     */
    val serializationStrategy: SerializationStrategy,

    /**
     * Compression pools that provide support for request decompression
     * and response compression.
     *
     * The first pool in the list is used for response compression if
     * the client indicates support.
     */
    compressionPools: List<CompressionPool> = listOf(GzipCompressionPool),

    /**
     * Set of server interceptors that should be invoked with requests/responses.
     */
    interceptors: List<(ServerConfig) -> ServerInterceptor> = emptyList(),

    /**
     * The coroutine context to use for handler execution.
     * If null, handlers run in the coroutine context of the HTTP server.
     */
    val handlerCoroutineContext: CoroutineContext? = null,

    /**
     * Whether to compress responses.
     * If true and the client indicates support, responses will be compressed.
     */
    val compressResponses: Boolean = true,

    /**
     * Minimum size in bytes for response compression.
     * Responses smaller than this will not be compressed.
     */
    val compressionMinBytes: Int = 1024,
) {
    private val compressionPoolsMap = mutableMapOf<String, CompressionPool>()
    private val interceptorList: List<ServerInterceptor>

    /**
     * The preferred compression method for responses.
     */
    val preferredCompression: CompressionPool? = compressionPools.firstOrNull()

    init {
        for (compressionPool in compressionPools) {
            compressionPoolsMap[compressionPool.name()] = compressionPool
        }
        interceptorList = interceptors.map { factory -> factory(this) }
    }

    /**
     * Get the compression pool by name.
     *
     * @param name The name of the compression pool.
     * @return The compression pool, or null if not found.
     */
    fun compressionPool(name: String?): CompressionPool? {
        if (name == null) return null
        return compressionPoolsMap[name]
    }

    /**
     * Get the names of supported compression methods.
     */
    fun supportedCompressionNames(): Set<String> {
        return compressionPoolsMap.keys
    }

    /**
     * Get the registered interceptors.
     */
    fun interceptors(): List<ServerInterceptor> {
        return interceptorList
    }
}

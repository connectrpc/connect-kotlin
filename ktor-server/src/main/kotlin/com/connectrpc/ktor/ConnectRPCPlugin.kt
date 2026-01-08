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

import com.connectrpc.SerializationStrategy
import com.connectrpc.compression.CompressionPool
import com.connectrpc.compression.GzipCompressionPool
import com.connectrpc.server.ConnectServer
import com.connectrpc.server.HandlerSpec
import com.connectrpc.server.ServerConfig
import com.connectrpc.server.ServiceHandler
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.BaseApplicationPlugin
import io.ktor.server.application.call
import io.ktor.util.AttributeKey
import kotlin.coroutines.CoroutineContext

/**
 * Ktor plugin for Connect RPC.
 *
 * Example usage:
 * ```kotlin
 * fun Application.module() {
 *     install(ConnectRPC) {
 *         serializationStrategy = GoogleJavaProtobufStrategy()
 *         handler(
 *             HandlerSpec(
 *                 procedure = "eliza.v1.ElizaService/Say",
 *                 requestClass = SayRequest::class,
 *                 responseClass = SayResponse::class,
 *                 streamType = StreamType.UNARY,
 *                 handler = UnaryHandler { ctx, req -> ... }
 *             )
 *         )
 *     }
 * }
 * ```
 */
class ConnectRPC private constructor(
    private val server: ConnectServer,
) {
    /**
     * Configuration for the ConnectRPC plugin.
     */
    class Configuration {
        /**
         * The serialization strategy for encoding/decoding messages.
         * This must be set before installing the plugin.
         */
        lateinit var serializationStrategy: SerializationStrategy

        /**
         * Compression pools for request decompression and response compression.
         */
        var compressionPools: List<CompressionPool> = listOf(GzipCompressionPool)

        /**
         * Whether to compress responses.
         */
        var compressResponses: Boolean = true

        /**
         * Minimum size in bytes for response compression.
         */
        var compressionMinBytes: Int = 1024

        /**
         * Coroutine context for handler execution.
         */
        var handlerCoroutineContext: CoroutineContext? = null

        private val handlers = mutableListOf<HandlerSpec<*, *>>()

        /**
         * Registers a handler specification.
         */
        fun handler(spec: HandlerSpec<*, *>) {
            handlers.add(spec)
        }

        /**
         * Registers multiple handler specifications.
         */
        fun handlers(vararg specs: HandlerSpec<*, *>) {
            handlers.addAll(specs)
        }

        /**
         * Registers all handlers from a service handler.
         */
        fun <T : ServiceHandler> service(
            serviceHandler: T,
            specsProvider: (T) -> List<HandlerSpec<*, *>>,
        ) {
            handlers.addAll(specsProvider(serviceHandler))
        }

        internal fun build(): ConnectServer {
            val config = ServerConfig(
                serializationStrategy = serializationStrategy,
                compressionPools = compressionPools,
                compressResponses = compressResponses,
                compressionMinBytes = compressionMinBytes,
                handlerCoroutineContext = handlerCoroutineContext,
            )
            val server = ConnectServer(config)
            server.registerHandlers(*handlers.toTypedArray())
            return server
        }
    }

    companion object Plugin : BaseApplicationPlugin<ApplicationCallPipeline, Configuration, ConnectRPC> {
        override val key = AttributeKey<ConnectRPC>("ConnectRPC")

        override fun install(pipeline: ApplicationCallPipeline, configure: Configuration.() -> Unit): ConnectRPC {
            val configuration = Configuration().apply(configure)
            val server = configuration.build()
            val plugin = ConnectRPC(server)

            pipeline.intercept(ApplicationCallPipeline.Call) {
                if (plugin.handleCall(call)) {
                    finish()
                }
            }

            return plugin
        }
    }

    /**
     * Handles an incoming Ktor call.
     *
     * @return true if the call was handled, false otherwise.
     */
    suspend fun handleCall(call: ApplicationCall): Boolean {
        val httpCall = KtorHTTPServerCall(call)
        return server.handle(httpCall)
    }

    /**
     * Gets all registered procedures.
     */
    fun registeredProcedures(): Set<String> {
        return server.registeredProcedures()
    }
}

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

import com.connectrpc.server.ConnectServer
import com.connectrpc.server.HandlerSpec
import com.connectrpc.server.ServerConfig
import com.connectrpc.server.ServiceHandler
import io.ktor.server.application.call
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route

/**
 * DSL for registering Connect RPC handlers within Ktor routing.
 *
 * Example usage:
 * ```kotlin
 * routing {
 *     connectRPC(serverConfig) {
 *         handler(myHandlerSpec)
 *         // or
 *         service(myServiceHandler) { it.asHandlerSpecs() }
 *     }
 * }
 * ```
 */
fun Route.connectRPC(
    config: ServerConfig,
    configure: ConnectRPCRouteConfiguration.() -> Unit,
) {
    connectRPCInternal(config, "", configure)
}

/**
 * Internal implementation for Connect RPC routing with optional base path.
 */
private fun Route.connectRPCInternal(
    config: ServerConfig,
    basePath: String,
    configure: ConnectRPCRouteConfiguration.() -> Unit,
) {
    val routeConfig = ConnectRPCRouteConfiguration().apply(configure)
    val server = ConnectServer(config)
    server.registerHandlers(*routeConfig.handlers.toTypedArray())

    // Register routes for each handler
    for (spec in routeConfig.handlers) {
        val path = "/${spec.procedure}"
        post(path) {
            val httpCall = KtorHTTPServerCall(call, basePath)
            server.handle(httpCall)
        }
    }
}

/**
 * Configuration for Connect RPC routing.
 */
class ConnectRPCRouteConfiguration {
    internal val handlers = mutableListOf<HandlerSpec<*, *>>()

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
}

/**
 * Convenience extension to mount Connect RPC at a specific path prefix.
 *
 * Example:
 * ```kotlin
 * routing {
 *     mountConnectRPC("/api", serverConfig) {
 *         handler(myHandlerSpec)
 *     }
 * }
 * ```
 */
fun Route.mountConnectRPC(
    pathPrefix: String,
    config: ServerConfig,
    configure: ConnectRPCRouteConfiguration.() -> Unit,
) {
    route(pathPrefix) {
        connectRPCInternal(config, pathPrefix, configure)
    }
}

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

import com.connectrpc.Code
import com.connectrpc.ConnectException
import com.connectrpc.StreamType
import com.connectrpc.server.http.HTTPServerCall
import com.connectrpc.server.http.ServerResponse
import com.connectrpc.server.protocols.ServerConnectProtocol
import com.connectrpc.server.protocols.ServerGRPCProtocol
import kotlinx.coroutines.withContext
import okio.Buffer
import kotlin.coroutines.coroutineContext

/**
 * Main entry point for handling Connect RPC requests.
 *
 * This class routes incoming HTTP calls to the appropriate handlers
 * and manages the protocol negotiation. Supports both Connect and gRPC protocols.
 */
class ConnectServer(
    private val config: ServerConfig,
) {
    private val handlers = mutableMapOf<String, HandlerSpec<*, *>>()
    private val connectProtocol = ServerConnectProtocol(config)
    private val grpcProtocol = ServerGRPCProtocol(config)

    /**
     * Registers handler specifications.
     *
     * @param specs The handler specifications to register.
     */
    fun registerHandlers(vararg specs: HandlerSpec<*, *>) {
        for (spec in specs) {
            handlers[spec.procedure] = spec
        }
    }

    /**
     * Registers all handlers from a service handler.
     *
     * @param serviceHandler The service handler containing RPC method implementations.
     * @param specsProvider A function that extracts handler specs from the service handler.
     */
    fun <T : ServiceHandler> registerService(
        serviceHandler: T,
        specsProvider: (T) -> List<HandlerSpec<*, *>>,
    ) {
        val specs = specsProvider(serviceHandler)
        registerHandlers(*specs.toTypedArray())
    }

    /**
     * Handles an incoming HTTP call.
     *
     * @param call The HTTP server call to handle.
     * @return true if the call was handled, false if no handler was found.
     */
    suspend fun handle(call: HTTPServerCall): Boolean {
        val procedure = call.path.removePrefix("/")
        val handler = handlers[procedure] ?: return false

        try {
            when (handler.streamType) {
                StreamType.UNARY -> handleUnary(call, handler)
                StreamType.SERVER -> handleServerStream(call, handler)
                StreamType.CLIENT, StreamType.BIDI -> {
                    // Not yet implemented
                    sendError(
                        call,
                        ConnectException(
                            code = Code.UNIMPLEMENTED,
                            message = "streaming not yet implemented",
                        ),
                    )
                }
            }
        } catch (e: ConnectException) {
            sendError(call, e)
        } catch (e: Exception) {
            sendError(
                call,
                ConnectException(
                    code = Code.INTERNAL_ERROR,
                    message = e.message ?: "internal error",
                    exception = e,
                ),
            )
        }

        return true
    }

    /**
     * Determines which protocol to use based on the request content type.
     */
    private fun isGRPCRequest(call: HTTPServerCall): Boolean {
        return grpcProtocol.canHandle(call)
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun <Input : Any, Output : Any> handleUnary(
        call: HTTPServerCall,
        handlerSpec: HandlerSpec<Input, Output>,
    ) {
        val handler = handlerSpec.handler as? UnaryHandler<Input, Output>
            ?: throw ConnectException(
                code = Code.INTERNAL_ERROR,
                message = "handler type mismatch",
            )

        val isGRPC = isGRPCRequest(call)

        // Parse request using appropriate protocol
        val request = if (isGRPC) {
            grpcProtocol.parseRequest(call)
        } else {
            connectProtocol.parseRequest(call)
        }

        // Deserialize request message
        val codec = config.serializationStrategy.codec(handlerSpec.requestClass)
        val inputMessage = codec.deserialize(request.message)

        // Create server context
        val context = ServerContextImpl(
            requestHeaders = request.headers,
            procedure = request.procedure,
            timeout = request.timeout,
            coroutineContext = config.handlerCoroutineContext ?: coroutineContext,
        )

        // Invoke handler
        val output = if (config.handlerCoroutineContext != null) {
            withContext(config.handlerCoroutineContext) {
                handler.handle(context, inputMessage)
            }
        } else {
            handler.handle(context, inputMessage)
        }

        // Serialize response
        val responseCodec = config.serializationStrategy.codec(handlerSpec.responseClass)
        val responseBuffer = responseCodec.serialize(output)

        // Send response
        val response = ServerResponse.Success(
            headers = context.responseHeaders,
            trailers = context.responseTrailers,
            message = responseBuffer,
        )

        // Send response using appropriate protocol
        if (isGRPC) {
            val acceptEncoding = grpcProtocol.getAcceptEncoding(request.headers)
            grpcProtocol.sendResponse(call, response, acceptEncoding)
        } else {
            val acceptEncoding = connectProtocol.getAcceptEncoding(request.headers)
            connectProtocol.sendResponse(call, response, acceptEncoding)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun <Input : Any, Output : Any> handleServerStream(
        call: HTTPServerCall,
        handlerSpec: HandlerSpec<Input, Output>,
    ) {
        // Server streaming is a future enhancement
        throw ConnectException(
            code = Code.UNIMPLEMENTED,
            message = "server streaming not yet implemented",
        )
    }

    private suspend fun sendError(call: HTTPServerCall, error: ConnectException) {
        val response = ServerResponse.Failure(
            error = error,
        )
        if (isGRPCRequest(call)) {
            grpcProtocol.sendResponse(call, response, emptyList())
        } else {
            connectProtocol.sendResponse(call, response, emptyList())
        }
    }

    /**
     * Finds a handler for the given procedure.
     *
     * @param procedure The procedure path (e.g., "package.Service/Method").
     * @return The handler spec, or null if not found.
     */
    fun findHandler(procedure: String): HandlerSpec<*, *>? {
        return handlers[procedure]
    }

    /**
     * Gets all registered procedures.
     */
    fun registeredProcedures(): Set<String> {
        return handlers.keys.toSet()
    }
}

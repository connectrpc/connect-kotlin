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

import com.connectrpc.Idempotency
import com.connectrpc.StreamType
import kotlinx.coroutines.flow.Flow
import kotlin.reflect.KClass

/**
 * Specification for an RPC handler.
 *
 * This class holds all the metadata needed to route and invoke an RPC method.
 * Generated code creates instances of this class for each RPC method.
 *
 * @param Input The request message type.
 * @param Output The response message type.
 */
class HandlerSpec<Input : Any, Output : Any>(
    /**
     * The full procedure path (e.g., "connectrpc.eliza.v1.ElizaService/Say").
     */
    val procedure: String,

    /**
     * The request message class.
     */
    val requestClass: KClass<Input>,

    /**
     * The response message class.
     */
    val responseClass: KClass<Output>,

    /**
     * The stream type (UNARY, CLIENT, SERVER, BIDI).
     */
    val streamType: StreamType,

    /**
     * The handler function to invoke for this RPC.
     */
    val handler: RPCHandler<Input, Output>,

    /**
     * The idempotency of this RPC method.
     */
    val idempotency: Idempotency = Idempotency.UNKNOWN,
) {
    /**
     * The service name extracted from the procedure path.
     */
    val serviceName: String
        get() = procedure.substringBeforeLast("/")

    /**
     * The method name extracted from the procedure path.
     */
    val methodName: String
        get() = procedure.substringAfterLast("/")
}

/**
 * Base interface for RPC handlers.
 *
 * @param Input The request message type.
 * @param Output The response message type.
 */
sealed interface RPCHandler<Input : Any, Output : Any>

/**
 * Handler for unary RPCs.
 *
 * Takes a single request and returns a single response.
 */
class UnaryHandler<Input : Any, Output : Any>(
    /**
     * The handler function.
     *
     * @param context The server context for this RPC.
     * @param request The request message.
     * @return The response message.
     */
    val handle: suspend (context: ServerContext, request: Input) -> Output,
) : RPCHandler<Input, Output>

/**
 * Handler for server streaming RPCs.
 *
 * Takes a single request and sends multiple responses via the response stream.
 */
class ServerStreamHandler<Input : Any, Output : Any>(
    /**
     * The handler function.
     *
     * @param context The server context for this RPC.
     * @param request The request message.
     * @param responses The stream to send response messages to.
     */
    val handle: suspend (context: ServerContext, request: Input, responses: ResponseStream<Output>) -> Unit,
) : RPCHandler<Input, Output>

/**
 * Handler for client streaming RPCs (future extension).
 *
 * Receives multiple requests and returns a single response.
 */
class ClientStreamHandler<Input : Any, Output : Any>(
    /**
     * The handler function.
     *
     * @param context The server context for this RPC.
     * @param requests The flow of request messages from the client.
     * @return The response message.
     */
    val handle: suspend (context: ServerContext, requests: Flow<Input>) -> Output,
) : RPCHandler<Input, Output>

/**
 * Handler for bidirectional streaming RPCs (future extension).
 *
 * Receives multiple requests and sends multiple responses.
 */
class BidiStreamHandler<Input : Any, Output : Any>(
    /**
     * The handler function.
     *
     * @param context The server context for this RPC.
     * @param requests The flow of request messages from the client.
     * @param responses The stream to send response messages to.
     */
    val handle: suspend (context: ServerContext, requests: Flow<Input>, responses: ResponseStream<Output>) -> Unit,
) : RPCHandler<Input, Output>

/**
 * Interface for sending streaming responses to the client.
 *
 * @param Output The response message type.
 */
interface ResponseStream<Output : Any> {
    /**
     * Sends a response message to the client.
     */
    suspend fun send(message: Output)
}

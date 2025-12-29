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

package com.connectrpc

import com.connectrpc.http.Cancelable

typealias Headers = Map<String, List<String>>
typealias Trailers = Map<String, List<String>>

/**
 * Primary interface consumed by generated RPCs to perform requests and streams.
 * The client itself is protocol-agnostic, but can be configured during initialization.
 */
interface ProtocolClientInterface {
    /**
     * Perform a unary (non-streaming) request.
     *
     * @param request The outbound request message.
     * @param headers The outbound request headers to include.
     * @param methodSpec The Method for RPC path.
     * @param onResult Closure called when a response or error is received.
     *
     * @return A `Cancelable` which provides the ability to cancel the outbound request.
     */
    fun <Input : Any, Output : Any> unary(
        request: Input,
        headers: Headers,
        methodSpec: MethodSpec<Input, Output>,
        onResult: (ResponseMessage<Output>) -> Unit,
    ): Cancelable

    /**
     * Perform a suspended unary (non-streaming) request.
     *
     * @param request The outbound request message.
     * @param headers The outbound request headers to include.
     * @param methodSpec The Method for RPC path.
     *
     * @return The ResponseMessage for the unary call.
     */
    suspend fun <Input : Any, Output : Any> unary(
        request: Input,
        headers: Headers,
        methodSpec: MethodSpec<Input, Output>,
    ): ResponseMessage<Output>

    /**
     * Perform a synchronous unary (non-streaming) request.
     *
     * @param request The outbound request message.
     * @param headers The outbound request headers to include.
     * @param methodSpec The Method for RPC path.
     *
     * @return The [UnaryBlockingCall] for the unary request.
     */
    fun <Input : Any, Output : Any> unaryBlocking(
        request: Input,
        headers: Headers,
        methodSpec: MethodSpec<Input, Output>,
    ): UnaryBlockingCall<Output>

    /**
     * Start a new bidirectional stream.
     *
     * @param headers The outbound request headers to include.
     * @param methodSpec The Method for RPC path.
     *
     * @return An interface for interacting with and sending data over the bidirectional stream.
     */
    suspend fun <Input : Any, Output : Any> stream(
        headers: Headers,
        methodSpec: MethodSpec<Input, Output>,
    ): BidirectionalStreamInterface<Input, Output>

    /**
     * Start a new server only stream.
     *
     * @param headers The outbound request headers to include.
     * @param methodSpec The Method for RPC path.
     *
     * @return An interface for interacting with and receiving data over the server only stream.
     */
    suspend fun <Input : Any, Output : Any> serverStream(
        headers: Headers,
        methodSpec: MethodSpec<Input, Output>,
    ): ServerOnlyStreamInterface<Input, Output>

    /**
     * Start a new client only stream.
     *
     * @param headers The outbound request headers to include.
     * @param methodSpec The Method for RPC path.
     *
     * @return An interface for interacting with and sending data over the client only stream.
     */
    suspend fun <Input : Any, Output : Any> clientStream(
        headers: Headers,
        methodSpec: MethodSpec<Input, Output>,
    ): ClientOnlyStreamInterface<Input, Output>
}

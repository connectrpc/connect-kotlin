// Copyright 2022-2023 The Connect Authors
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

import kotlinx.coroutines.channels.ReceiveChannel

/**
 * Represents a server-only stream (a stream where the server streams data to the client after
 * receiving an initial request) that can send request messages.
 */
interface ServerOnlyStreamInterface<Input, Output> {
    /**
     * The Channel for received StreamResults.
     *
     * @return ReceiveChannel for iterating over the received results.
     */
    fun responseChannel(): ReceiveChannel<Output>

    /**
     * Send a request to the server over the stream and closes the request.
     *
     * Can only be called exactly one time when starting the stream.
     *
     * @param input The request message to send.
     * @return [Result.success] on send success, [Result.failure] on
     *         any sends which are not successful.
     */
    suspend fun sendAndClose(input: Input): Result<Unit>

    /**
     * Close the receive stream.
     */
    fun receiveClose()

    /**
     * Determine if the underlying client receive stream is closed.
     *
     * @return true if the underlying client receive stream is closed. If the stream is still open,
     *         this will return false.
     */
    fun isReceiveClosed(): Boolean

    /**
     * Determine if the underlying stream is closed.
     *
     * @return true if the underlying stream is closed. If the stream is still open,
     *         this will return false.
     */
    fun isClosed(): Boolean
}

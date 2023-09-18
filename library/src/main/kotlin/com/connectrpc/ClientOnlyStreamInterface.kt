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

/**
 * Represents a client-only stream (a stream where the client streams data to the server and
 * eventually receives a response) that can send request messages and initiate closes.
 */
interface ClientOnlyStreamInterface<Input, Output> {
    /**
     * Send a request to the server over the stream.
     *
     * @param input The request message to send.
     */
    suspend fun send(input: Input): Result<Unit>

    /**
     * Receive a single response and close the stream.
     *
     * @return the single response [StreamResult].
     */
    suspend fun receiveAndClose(): StreamResult<Output>

    /**
     * Close the stream. No calls to [send] are valid after calling [close].
     */
    fun close()

    /**
     * Determine if the underlying stream is closed.
     *
     * @return true if the underlying stream is closed. If the stream is still open,
     *         this will return false.
     */
    fun isClosed(): Boolean
}

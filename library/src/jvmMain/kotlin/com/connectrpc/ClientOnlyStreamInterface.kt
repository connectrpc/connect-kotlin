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

import kotlinx.coroutines.Deferred

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
     * @return the single response [Output].
     * @throws ConnectException If an error occurs making the call or processing the response.
     */
    suspend fun receiveAndClose(): Output

    /**
     * The response headers. This value will become available before any call to
     * [receiveAndClose] completes and before trailers are available from
     * [responseTrailers] (though these may occur nearly simultaneously). If the
     * stream fails before headers are ever received, this will complete with an
     * empty value. The [receiveAndClose] method can be used to recover the
     * exception that caused such a failure.
     */
    fun responseHeaders(): Deferred<Headers>

    /**
     * The response trailers. This value will not become available until the entire
     * RPC operation is complete. If the stream fails before trailers are ever
     * received, this will complete with an empty value. The [receiveAndClose]
     * method can be used to recover the exception that caused such a failure.
     */
    fun responseTrailers(): Deferred<Headers>

    /**
     * Close the stream. No calls to [send] are valid after calling [sendClose].
     */
    suspend fun sendClose()

    /**
     * Cancels the stream. This closes both send and receive sides of the stream
     * without awaiting any server reply.
     */
    suspend fun cancel()

    /**
     * Determine if the underlying client send stream is closed.
     *
     * @return true if the underlying client receive stream is closed. If the stream is still open,
     *         this will return false.
     */
    fun isSendClosed(): Boolean

    /**
     * Determine if the underlying stream is closed.
     *
     * @return true if the underlying stream is closed. If the stream is still open,
     *         this will return false.
     */
    fun isClosed(): Boolean
}

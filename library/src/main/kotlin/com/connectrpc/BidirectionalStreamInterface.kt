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
import kotlinx.coroutines.channels.ReceiveChannel

/**
 * Represents a bidirectional stream that can send request messages and initiate closes.
 */
interface BidirectionalStreamInterface<Input, Output> {
    /**
     * The Channel for responses.
     *
     * @return ReceiveChannel for iterating over the responses.
     */
    fun responseChannel(): ReceiveChannel<Output>

    /**
     * The response headers. This value will become available before any output
     * messages become available from the [responseChannel] and before trailers
     * are available from [responseTrailers]. If the stream fails before headers
     * are ever received, this will complete with an empty value. The
     * [ReceiveChannel.receive] method of [responseChannel] can be used to
     * recover the exception that caused such a failure.
     */
    fun responseHeaders(): Deferred<Headers>

    /**
     * The response trailers. This value will not become available until the entire
     * RPC operation is complete. If the stream fails before trailers are ever
     * received, this will complete with an empty value. The [ReceiveChannel.receive]
     * method of [responseChannel] can be used to recover the exception that caused
     * such a failure.
     */
    fun responseTrailers(): Deferred<Headers>

    /**
     * Send a request to the server over the stream.
     *
     * @param input The request message to send.
     * @return [Result.success] on send success, [Result.failure] on
     *         any sends which are not successful.
     */
    suspend fun send(input: Input): Result<Unit>

    /**
     * Determine if the underlying send and receive stream is closed.
     *
     * @return true if the underlying send and receive stream is closed. If the stream is still open,
     *         this will return false.
     */
    fun isClosed(): Boolean

    /**
     * Close the send stream. No calls to [send] are valid after calling [sendClose].
     */
    suspend fun sendClose()

    /**
     * Close the receive stream.
     */
    suspend fun receiveClose()

    /**
     * Determine if the underlying client send stream is closed.
     *
     * @return true if the underlying client receive stream is closed. If the stream is still open,
     *         this will return false.
     */
    fun isSendClosed(): Boolean

    /**
     * Determine if the underlying client receive stream is closed.
     *
     * @return true if the underlying client receive stream is closed. If the stream is still open,
     *         this will return false.
     */
    fun isReceiveClosed(): Boolean
}

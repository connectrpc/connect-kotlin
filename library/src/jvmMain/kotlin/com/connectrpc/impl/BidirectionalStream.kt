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

package com.connectrpc.impl

import com.connectrpc.BidirectionalStreamInterface
import com.connectrpc.Codec
import com.connectrpc.Headers
import com.connectrpc.http.Stream
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import java.lang.Exception

/**
 * Concrete implementation of [BidirectionalStreamInterface].
 */
internal class BidirectionalStream<Input, Output>(
    val stream: Stream,
    private val requestCodec: Codec<Input>,
    private val responseChannel: Channel<Output>,
    private val responseHeaders: Deferred<Headers>,
    private val responseTrailers: Deferred<Headers>,
) : BidirectionalStreamInterface<Input, Output> {

    override suspend fun send(input: Input): Result<Unit> {
        val msg = try {
            requestCodec.serialize(input)
        } catch (e: Exception) {
            return Result.failure(e)
        }
        return stream.send(msg)
    }

    override fun responseChannel(): ReceiveChannel<Output> {
        return responseChannel
    }

    override fun responseHeaders(): Deferred<Headers> {
        return responseHeaders
    }

    override fun responseTrailers(): Deferred<Headers> {
        return responseTrailers
    }

    override fun isClosed(): Boolean {
        return stream.isReceiveClosed()
    }

    override suspend fun sendClose() {
        stream.sendClose()
    }

    override suspend fun receiveClose() {
        stream.receiveClose()
    }

    override fun isSendClosed(): Boolean {
        return stream.isSendClosed()
    }

    override fun isReceiveClosed(): Boolean {
        return stream.isReceiveClosed()
    }
}

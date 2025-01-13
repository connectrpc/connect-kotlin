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

package com.connectrpc.conformance.client.adapt

import com.connectrpc.BidirectionalStreamInterface
import com.connectrpc.Headers
import com.connectrpc.ServerOnlyStreamInterface
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.ChannelIterator
import kotlinx.coroutines.channels.ChannelResult
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.selects.SelectClause1

/**
 * ResponseStream is a stream that allows a client to download
 * zero or more request messages. Typically, the client should
 * keep receiving messages until the end of the stream is reached.
 * If the client closes the response stream before consuming all
 * messages, the associated streaming RPC operation is cancelled.
 *
 * @param Resp The response message type
 */
interface ResponseStream<Resp> :
    ReceiveChannel<Resp>,
    SuspendCloseable {

    suspend fun headers(): Headers
    suspend fun trailers(): Headers

    @Deprecated(
        """Prefer close() instead. Since response streams are not buffered,
        there will not be undelivered items to discard. The close() method
        will result in a ConnectException with a CANCELED code being thrown
        by calls to receive, whereas this method results in the given
        CancellationException being thrown.""",
        ReplaceWith("stream.close()"),
    )
    override fun cancel(cause: CancellationException?)

    companion object {
        fun <Req, Resp> new(underlying: BidirectionalStreamInterface<Req, Resp>): ResponseStream<Resp> {
            val channel = underlying.responseChannel()
            return object : ResponseStream<Resp> {
                override suspend fun headers(): Headers {
                    return underlying.responseHeaders().await()
                }

                override suspend fun trailers(): Headers {
                    return underlying.responseTrailers().await()
                }

                override suspend fun close() {
                    underlying.receiveClose()
                }

                @OptIn(DelicateCoroutinesApi::class)
                override val isClosedForReceive: Boolean
                    get() = channel.isClosedForReceive

                @ExperimentalCoroutinesApi
                override val isEmpty: Boolean
                    get() = channel.isEmpty

                override val onReceive: SelectClause1<Resp>
                    get() = channel.onReceive

                override val onReceiveCatching: SelectClause1<ChannelResult<Resp>>
                    get() = channel.onReceiveCatching

                @Deprecated("Since 1.2.0, binary compatibility with versions <= 1.1.x", level = DeprecationLevel.HIDDEN)
                override fun cancel(cause: Throwable?): Boolean {
                    channel.cancel(CancellationException())
                    return false
                }

                @Deprecated("Prefer close() instead.", ReplaceWith("stream.close()"))
                override fun cancel(cause: CancellationException?) {
                    channel.cancel(cause)
                }

                override fun iterator(): ChannelIterator<Resp> {
                    return channel.iterator()
                }

                override suspend fun receive(): Resp {
                    return channel.receive()
                }

                override suspend fun receiveCatching(): ChannelResult<Resp> {
                    return channel.receiveCatching()
                }

                override fun tryReceive(): ChannelResult<Resp> {
                    return channel.tryReceive()
                }
            }
        }

        fun <Req, Resp> new(underlying: ServerOnlyStreamInterface<Req, Resp>): ResponseStream<Resp> {
            val channel = underlying.responseChannel()
            return object : ResponseStream<Resp> {
                override suspend fun headers(): Headers {
                    return underlying.responseHeaders().await()
                }

                override suspend fun trailers(): Headers {
                    return underlying.responseTrailers().await()
                }

                override suspend fun close() {
                    underlying.receiveClose()
                }

                @OptIn(DelicateCoroutinesApi::class)
                override val isClosedForReceive: Boolean
                    get() = channel.isClosedForReceive

                @ExperimentalCoroutinesApi
                override val isEmpty: Boolean
                    get() = channel.isEmpty

                override val onReceive: SelectClause1<Resp>
                    get() = channel.onReceive

                override val onReceiveCatching: SelectClause1<ChannelResult<Resp>>
                    get() = channel.onReceiveCatching

                @Deprecated("Since 1.2.0, binary compatibility with versions <= 1.1.x", level = DeprecationLevel.HIDDEN)
                override fun cancel(cause: Throwable?): Boolean {
                    channel.cancel(CancellationException())
                    return false
                }

                @Deprecated("Prefer close() instead.", ReplaceWith("stream.close()"))
                override fun cancel(cause: CancellationException?) {
                    channel.cancel(cause)
                }

                override fun iterator(): ChannelIterator<Resp> {
                    return channel.iterator()
                }

                override suspend fun receive(): Resp {
                    return channel.receive()
                }

                override suspend fun receiveCatching(): ChannelResult<Resp> {
                    return channel.receiveCatching()
                }

                override fun tryReceive(): ChannelResult<Resp> {
                    return channel.tryReceive()
                }
            }
        }
    }
}

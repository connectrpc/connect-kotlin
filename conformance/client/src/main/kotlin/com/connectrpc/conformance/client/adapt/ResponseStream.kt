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

package com.connectrpc.conformance.client.adapt

import com.connectrpc.BidirectionalStreamInterface
import com.connectrpc.Headers
import com.connectrpc.ServerOnlyStreamInterface
import com.google.protobuf.MessageLite
import kotlinx.coroutines.channels.ReceiveChannel

/**
 * ResponseStream is a stream that allows a client to download
 * zero or more request messages. Typically, the client should
 * keep receiving messages until the end of the stream is reached.
 * If the client closes the response stream before consuming all
 * messages, the associated streaming RPC operation is cancelled.
 *
 * @param Resp The response message type
 */
interface ResponseStream<Resp : MessageLite> : Closeable {
    val messages: ReceiveChannel<Resp>
    suspend fun headers(): Headers
    suspend fun trailers(): Headers

    companion object {
        fun <Req : MessageLite, Resp : MessageLite> new(underlying: BidirectionalStreamInterface<Req, Resp>): ResponseStream<Resp> {
            return object : ResponseStream<Resp> {
                override val messages: ReceiveChannel<Resp>
                    get() = underlying.responseChannel()

                override suspend fun headers(): Headers {
                    return underlying.responseHeaders().await()
                }

                override suspend fun trailers(): Headers {
                    return underlying.responseTrailers().await()
                }

                override suspend fun close() {
                    underlying.receiveClose()
                }
            }
        }

        fun <Req : MessageLite, Resp : MessageLite> new(underlying: ServerOnlyStreamInterface<Req, Resp>): ResponseStream<Resp> {
            return object : ResponseStream<Resp> {
                override val messages: ReceiveChannel<Resp>
                    get() = underlying.responseChannel()

                override suspend fun headers(): Headers {
                    return underlying.responseHeaders().await()
                }

                override suspend fun trailers(): Headers {
                    return underlying.responseTrailers().await()
                }

                override suspend fun close() {
                    underlying.receiveClose()
                }
            }
        }
    }
}

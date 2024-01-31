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
import com.google.protobuf.MessageLite

/**
 * RequestStream is a stream that allows a client to upload
 * zero or more request messages. When the client is done
 * sending messages, it must close the stream.
 *
 * Note that closing the request stream is not strictly
 * required if the RPC is cancelled or fails prematurely
 * or if the response stream is closed first. Closing the
 * requests "half-closes" the stream; closing the responses
 * "fully closes" it.
 */
interface RequestStream<Req : MessageLite> : Closeable {
    /**
     * Sends a message on the stream.
     * @throws Exception when the request cannot be sent
     *         because of an error with the streaming call
     */
    suspend fun send(req: Req)

    companion object {
        fun <Req : MessageLite, Resp : MessageLite> new(underlying: BidirectionalStreamInterface<Req, Resp>): RequestStream<Req> {
            return object : RequestStream<Req> {
                override suspend fun send(req: Req) {
                    val result = underlying.send(req)
                    if (result.isFailure) {
                        throw result.exceptionOrNull()!!
                    }
                }

                override suspend fun close() {
                    underlying.sendClose()
                }
            }
        }
    }
}

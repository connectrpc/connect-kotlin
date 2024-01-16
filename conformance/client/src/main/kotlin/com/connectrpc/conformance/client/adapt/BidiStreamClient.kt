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
import com.google.protobuf.MessageLite

/**
 * The client of a bidi-stream RPC operation. A bidi-stream
 * operation allows the client to upload zero or more request
 * messages and to download zero or more response messages.
 * Furthermore, bidi-stream operations can be "full duplex",
 * which means that sending requests can be interleaved with
 * receiving responses (whereas other stream types always
 * require sending the request(s) first, and then receiving
 * the response(s)).
 *
 * @param Req The request message type
 * @param Resp The response message type
 */
abstract class BidiStreamClient<Req : MessageLite, Resp : MessageLite>(
    val reqTemplate: Req,
    val respTemplate: Resp,
) {
    abstract suspend fun execute(headers: Headers): BidiStream<Req, Resp>

    /**
     * A BidiStream combines a request stream and a response stream.
     *
     * @param Req The request message type
     * @param Resp The response message type
     */
    interface BidiStream<Req : MessageLite, Resp : MessageLite> {
        val requests: RequestStream<Req>
        val responses: ResponseStream<Resp>
        companion object {
            fun <Req : MessageLite, Resp : MessageLite> new(underlying: BidirectionalStreamInterface<Req, Resp>): BidiStream<Req, Resp> {
                val reqStream = RequestStream.new(underlying)
                val respStream = ResponseStream.new(underlying)
                return object : BidiStream<Req, Resp> {
                    override val requests: RequestStream<Req>
                        get() = reqStream

                    override val responses: ResponseStream<Resp>
                        get() = respStream
                }
            }
        }
    }
}

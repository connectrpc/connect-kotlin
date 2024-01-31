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

import com.connectrpc.Headers
import com.google.protobuf.MessageLite

/**
 * The client of a server-stream RPC operation. A server-stream
 * operation allows the client to send a single request and then
 * download zero or more response messages.
 *
 * @param Req The request message type
 * @param Resp The response message type
 */
abstract class ServerStreamClient<Req : MessageLite, Resp : MessageLite>(
    val reqTemplate: Req,
    val respTemplate: Resp,
) {
    abstract suspend fun execute(req: Req, headers: Headers): ResponseStream<Resp>
}

/**
 * Executes the server-stream call inside the given block. The block
 * is used to consume the responses. The stream is automatically closed
 * when the block returns or throws.
 */
suspend fun <Req : MessageLite, Resp : MessageLite, R> ServerStreamClient<Req, Resp>.execute(
    req: Req,
    headers: Headers,
    block: suspend (ResponseStream<Resp>) -> R,
): R {
    val stream = execute(req, headers)
    return stream.use(block)
}

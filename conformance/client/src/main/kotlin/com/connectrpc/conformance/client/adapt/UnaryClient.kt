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

import com.connectrpc.Headers
import com.connectrpc.ResponseMessage
import com.connectrpc.UnaryBlockingCall
import com.connectrpc.http.Cancelable
import com.google.protobuf.MessageLite

/**
 * The client of a unary RPC operation. This provides multiple ways
 * to invoke the RPC: suspend-based async, callback-based async, or
 * blocking.
 *
 * @param Req The request message type
 * @param Resp The response message type
 */
abstract class UnaryClient<Req : MessageLite, Resp : MessageLite>(
    val reqTemplate: Req,
    val respTemplate: Resp,
) {
    abstract suspend fun execute(req: Req, headers: Headers): ResponseMessage<Resp>

    abstract fun execute(req: Req, headers: Headers, onFinish: (ResponseMessage<Resp>) -> Unit): Cancelable

    abstract fun blocking(req: Req, headers: Headers): UnaryBlockingCall<Resp>
}

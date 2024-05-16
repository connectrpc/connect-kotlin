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

import com.connectrpc.CallOptions
import com.connectrpc.ResponseMessage
import com.connectrpc.UnaryBlockingCall
import com.connectrpc.http.Cancelable
import com.google.protobuf.MessageLite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

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
    abstract suspend fun execute(req: Req, options: CallOptions = CallOptions.empty): ResponseMessage<Resp>

    abstract fun execute(req: Req, options: CallOptions = CallOptions.empty, onFinish: (ResponseMessage<Resp>) -> Unit): Cancelable

    abstract fun blocking(req: Req, options: CallOptions = CallOptions.empty): UnaryBlockingCall<Resp>

    /**
     * Executes the unary RPC using the given invocation style, request
     * message, and request headers. The given callback is invoked when
     * the operation completes.
     *
     * This signature resembles the one above that takes a callback, but
     * will adapt the call to the suspend or blocking signatures if so
     * directed by the given InvokeStyle. This allows a caller to use a
     * single shape to invoke the RPC, but actually exercise any/all of
     * the above three signatures.
     */
    suspend fun execute(
        style: InvokeStyle,
        req: Req,
        options: CallOptions = CallOptions.empty,
        onFinish: (ResponseMessage<Resp>) -> Unit,
    ): Cancelable {
        when (style) {
            InvokeStyle.CALLBACK -> {
                return execute(req, options, onFinish)
            }
            InvokeStyle.SUSPEND -> {
                return coroutineScope {
                    val job = launch {
                        onFinish(execute(req, options))
                    }
                    return@coroutineScope {
                        job.cancel()
                    }
                }
            }
            InvokeStyle.BLOCKING -> {
                val call = blocking(req, options)
                coroutineScope {
                    launch(Dispatchers.IO) {
                        onFinish(call.execute())
                    }
                }
                return {
                    call.cancel()
                }
            }
        }
    }

    /**
     * The style of invocation, one each for the three different
     * ways to invoke a unary RPC.
     */
    enum class InvokeStyle {
        /**
         * Indicates the callback-based async signature, which
         * invokes the method with the following signature:
         * ```
         * fun execute(Req, Headers, (ResponseMessage<Resp>)->Unit): Cancelable
         * ```
         */
        CALLBACK,

        /**
         * Indicates the suspend-based async signature, which
         * invokes the method with the following signature:
         * ```
         * suspend fun execute(Req, Headers): ResponseMessage<Resp>
         * ```
         */
        SUSPEND,

        /**
         * Indicates the blocking signature, which invokes the
         * method with the following signature:
         * ```
         * fun blocking(Req, Headers): UnaryBlockingCall<Resp>
         * ```
         */
        BLOCKING,
    }
}

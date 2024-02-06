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

import com.connectrpc.ClientOnlyStreamInterface
import com.connectrpc.Code
import com.connectrpc.ConnectException
import com.connectrpc.Headers
import com.connectrpc.ResponseMessage
import com.google.protobuf.MessageLite
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.coroutineScope

/**
 * The client of a client-stream RPC operation. A client-stream
 * operation allows the client to upload zero or more request
 * messages and then receive either a single response or an
 * error when done.
 *
 * @param Req The request message type
 * @param Resp The response message type
 */
abstract class ClientStreamClient<Req : MessageLite, Resp : MessageLite>(
    val reqTemplate: Req,
    val respTemplate: Resp,
) {
    /**
     * Executes the client-stream call inside the given block. The block
     * is used to send the requests and then retrieve the responses. The
     * stream is automatically closed when the block returns or throws.
     */
    suspend fun <R> execute(
        headers: Headers,
        block: suspend CoroutineScope.(ClientStream<Req, Resp>) -> R,
    ): R {
        val stream = execute(headers)
        return stream.use {
            coroutineScope { block(this, it) }
        }
    }

    protected abstract suspend fun execute(headers: Headers): ClientStream<Req, Resp>

    /**
     * A ClientStream is just like a RequestStream, except that closing
     * the stream waits for the operation result.
     *
     * @param Req The request message type
     * @param Resp The response message type
     */
    interface ClientStream<Req, Resp> :
        RequestStream<Req>,
        SuspendCloseable {

        /**
         * Closes the request stream **and** cancels the RPC. To close
         * the request stream normally, without canceling the RPC,
         * call closeAndReceive() instead.
         */
        override suspend fun close()

        /**
         * Closes the request stream and then awaits and returns
         * the RPC result.
         */
        suspend fun closeAndReceive(): ResponseMessage<Resp>

        companion object {
            fun <Req, Resp> new(underlying: ClientOnlyStreamInterface<Req, Resp>): ClientStream<Req, Resp> {
                return object : ClientStream<Req, Resp> {
                    override val isClosedForSend: Boolean
                        get() = underlying.isSendClosed()

                    override suspend fun send(req: Req) {
                        val result = underlying.send(req)
                        if (result.isFailure) {
                            throw result.exceptionOrNull()!!
                        }
                    }

                    override suspend fun closeAndReceive(): ResponseMessage<Resp> {
                        try {
                            val resp = underlying.receiveAndClose()
                            return ResponseMessage.Success(
                                message = resp,
                                code = Code.OK,
                                headers = underlying.responseHeaders().await(),
                                trailers = underlying.responseTrailers().await(),
                            )
                        } catch (e: Exception) {
                            val connectException = if (e is ConnectException) {
                                e
                            } else {
                                ConnectException(code = Code.UNKNOWN, exception = e)
                            }
                            return ResponseMessage.Failure(
                                cause = connectException,
                                code = connectException.code,
                                headers = underlying.responseHeaders().await(),
                                trailers = connectException.metadata,
                            )
                        }
                    }

                    override suspend fun close() {
                        underlying.cancel()
                    }
                }
            }
        }
    }
}

/**
 * Copies the contents of the given channel to the given
 * stream, closing the stream at the end and returning
 * the RPC result.
 */
suspend fun <Req, Resp> copyFromChannel(
    stream: ClientStreamClient.ClientStream<Req, Resp>,
    requests: ReceiveChannel<Req>,
): ResponseMessage<Resp> {
    return copyFromChannel(stream, requests) { it }
}

/**
 * Copies the contents of the given channel to the given
 * stream, transforming each element using the given lambda,
 * closing the stream at the end and returning the RPC result.
 */
suspend fun <Req, Resp, T> copyFromChannel(
    stream: ClientStreamClient.ClientStream<Req, Resp>,
    requests: ReceiveChannel<T>,
    toRequest: (T) -> Req,
): ResponseMessage<Resp> {
    stream.use {
        try {
            for (req in requests) {
                it.send(toRequest(req))
            }
            return it.closeAndReceive()
        } catch (ex: Throwable) {
            try {
                stream.close()
            } catch (closeEx: Throwable) {
                ex.addSuppressed(closeEx)
            }
            throw ex
        }
    }
}

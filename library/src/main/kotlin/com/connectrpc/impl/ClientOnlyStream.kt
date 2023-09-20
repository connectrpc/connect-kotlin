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

package com.connectrpc.impl

import com.connectrpc.BidirectionalStreamInterface
import com.connectrpc.ClientOnlyStreamInterface
import com.connectrpc.Code
import com.connectrpc.ConnectError
import com.connectrpc.Headers
import com.connectrpc.ResponseMessage

/**
 * Concrete implementation of [ClientOnlyStreamInterface].
 */
internal class ClientOnlyStream<Input, Output>(
    private val messageStream: BidirectionalStreamInterface<Input, Output>,
) : ClientOnlyStreamInterface<Input, Output> {
    override suspend fun send(input: Input): Result<Unit> {
        return messageStream.send(input)
    }

    override suspend fun receiveAndClose(): ResponseMessage<Output> {
        val resultChannel = messageStream.resultChannel()
        try {
            messageStream.sendClose()
            // TODO: Improve this API for consumers.
            // We should aim to provide ease of use for callers so they don't need to individually examine each result
            // in the channel (headers, 1* messages, completion) and have to resort to fold()/maybeFold() to interpret
            // the overall results.
            // Additionally, ResponseMessage.Success and ResponseMessage.Failure shouldn't be necessary for client use.
            // We should throw ConnectError for failure and only have users have to deal with success messages.
            var headers: Headers = emptyMap()
            var message: Output? = null
            var trailers: Headers = emptyMap()
            var code: Code? = null
            var error: ConnectError? = null
            for (result in resultChannel) {
                result.maybeFold(
                    onHeaders = {
                        headers = it.headers
                    },
                    onMessage = {
                        message = it.message
                    },
                    onCompletion = {
                        val connectError = it.connectError()
                        if (connectError != null) {
                            if (error != null) {
                                error!!.addSuppressed(connectError)
                            } else {
                                error = connectError
                            }
                        }
                        code = it.code
                        trailers = it.trailers
                    },
                )
            }
            if (error != null) {
                return ResponseMessage.Failure(error!!, code ?: Code.UNKNOWN, headers, trailers)
            }
            if (code == null) {
                return ResponseMessage.Failure(ConnectError(Code.UNKNOWN, message = "unknown status code"), Code.UNKNOWN, headers, trailers)
            }
            if (message != null) {
                return ResponseMessage.Success(message!!, code!!, headers, trailers)
            }
            // We didn't receive an error at any point, however we didn't get a response message either.
            return ResponseMessage.Failure(
                ConnectError(Code.UNKNOWN, message = "missing response message"),
                code!!,
                headers,
                trailers,
            )
        } finally {
            resultChannel.cancel()
        }
    }

    override fun sendClose() {
        return messageStream.sendClose()
    }

    override fun isSendClosed(): Boolean {
        return messageStream.isSendClosed()
    }

    override fun isClosed(): Boolean {
        return messageStream.isClosed()
    }
}

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
import com.connectrpc.ConnectException
import com.connectrpc.Headers
import kotlinx.coroutines.Deferred

/**
 * Concrete implementation of [ClientOnlyStreamInterface].
 */
internal class ClientOnlyStream<Input, Output>(
    private val messageStream: BidirectionalStreamInterface<Input, Output>,
) : ClientOnlyStreamInterface<Input, Output> {
    override suspend fun send(input: Input): Result<Unit> {
        return messageStream.send(input)
    }

    override suspend fun receiveAndClose(): Output {
        val resultChannel = messageStream.responseChannel()
        try {
            messageStream.sendClose()
            val message = resultChannel.receive()
            val additionalMessage = resultChannel.receiveCatching()
            if (additionalMessage.isSuccess) {
                throw ConnectException(code = Code.UNKNOWN, message = "unary stream has multiple messages")
            }
            return message
        } finally {
            resultChannel.cancel()
        }
    }

    override fun responseHeaders(): Deferred<Headers> {
        return messageStream.responseHeaders()
    }

    override fun responseTrailers(): Deferred<Headers> {
        return messageStream.responseTrailers()
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

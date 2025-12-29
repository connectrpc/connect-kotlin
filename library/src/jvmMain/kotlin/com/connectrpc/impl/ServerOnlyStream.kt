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

package com.connectrpc.impl

import com.connectrpc.BidirectionalStreamInterface
import com.connectrpc.Headers
import com.connectrpc.ServerOnlyStreamInterface
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.channels.ReceiveChannel

/**
 * Concrete implementation of [ServerOnlyStreamInterface].
 */
internal class ServerOnlyStream<Input, Output>(
    private val messageStream: BidirectionalStreamInterface<Input, Output>,
) : ServerOnlyStreamInterface<Input, Output> {
    override fun responseChannel(): ReceiveChannel<Output> {
        return messageStream.responseChannel()
    }

    override fun responseHeaders(): Deferred<Headers> {
        return messageStream.responseHeaders()
    }

    override fun responseTrailers(): Deferred<Headers> {
        return messageStream.responseTrailers()
    }

    override suspend fun sendAndClose(input: Input): Result<Unit> {
        try {
            return messageStream.send(input)
        } finally {
            messageStream.sendClose()
        }
    }

    override suspend fun receiveClose() {
        messageStream.receiveClose()
    }

    override fun isReceiveClosed(): Boolean {
        return messageStream.isReceiveClosed()
    }

    override fun isClosed(): Boolean {
        return messageStream.isClosed()
    }
}

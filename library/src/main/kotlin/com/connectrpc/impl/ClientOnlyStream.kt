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
import com.connectrpc.StreamResult

/**
 * Concrete implementation of [ClientOnlyStreamInterface].
 */
internal class ClientOnlyStream<Input, Output>(
    private val messageStream: BidirectionalStreamInterface<Input, Output>,
) : ClientOnlyStreamInterface<Input, Output> {
    override suspend fun send(input: Input): Result<Unit> {
        return messageStream.send(input)
    }

    override suspend fun receiveAndClose(): StreamResult<Output> {
        val resultChannel = messageStream.resultChannel()
        try {
            return resultChannel.receive()
        } finally {
            resultChannel.cancel()
        }
    }

    override fun close() {
        messageStream.close()
    }

    override fun isClosed(): Boolean {
        return messageStream.isClosed()
    }
}

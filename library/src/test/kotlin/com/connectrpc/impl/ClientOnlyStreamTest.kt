// Copyright 2022-2026 The Connect Authors
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
import com.connectrpc.Code
import com.connectrpc.ConnectException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowableOfType
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class ClientOnlyStreamTest {
    @Test
    fun receiveAndCloseReturnsSingleMessage() = runBlocking<Unit> {
        val channel = Channel<String>(Channel.UNLIMITED)
        channel.send("response")
        channel.close()
        val stream = ClientOnlyStream(bidiStream(channel))

        assertThat(stream.receiveAndClose()).isEqualTo("response")
    }

    @Test
    fun receiveAndCloseThrowsErrorAfterMessage() = runBlocking<Unit> {
        val channel = Channel<String>(Channel.UNLIMITED)
        channel.send("response")
        channel.close(ConnectException(code = Code.INTERNAL_ERROR, message = "missing trailers"))
        val stream = ClientOnlyStream(bidiStream(channel))

        val ex = catchThrowableOfType(ConnectException::class.java) {
            runBlocking { stream.receiveAndClose() }
        }
        assertThat(ex.code).isEqualTo(Code.INTERNAL_ERROR)
        assertThat(ex.message).isEqualTo("missing trailers")
    }

    @Test
    fun receiveAndCloseThrowsOnMultipleMessages() = runBlocking<Unit> {
        val channel = Channel<String>(Channel.UNLIMITED)
        channel.send("first")
        channel.send("second")
        channel.close()
        val stream = ClientOnlyStream(bidiStream(channel))

        val ex = catchThrowableOfType(ConnectException::class.java) {
            runBlocking { stream.receiveAndClose() }
        }
        assertThat(ex.code).isEqualTo(Code.UNIMPLEMENTED)
    }

    private fun bidiStream(channel: Channel<String>): BidirectionalStreamInterface<String, String> {
        val stream = mock<BidirectionalStreamInterface<String, String>>()
        whenever(stream.responseChannel()).thenReturn(channel)
        return stream
    }
}

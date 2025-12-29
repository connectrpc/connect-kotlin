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

import com.connectrpc.Code
import com.connectrpc.ConnectException
import com.connectrpc.ResponseMessage
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

class UnaryCallTest {
    @Test
    fun testExecute() {
        val executor = Executors.newSingleThreadExecutor()
        try {
            val result = Object()
            val call = UnaryCall<Any> { callback ->
                executor.execute {
                    callback.invoke(
                        ResponseMessage.Success(
                            result,
                            headers = emptyMap(),
                            trailers = emptyMap(),
                        ),
                    )
                }
                return@UnaryCall { }
            }
            val resp = call.execute()
            assertThat(resp).isInstanceOf(ResponseMessage.Success::class.java)
            val msg = resp.success { it.message }!!
            assertThat(msg).isEqualTo(result)
        } finally {
            assertThat(executor.shutdownNow()).isEmpty()
        }
    }

    @Test
    fun testCancelAfterExecute() {
        testCancel(false)
    }

    @Test
    fun testCancelBeforeExecute() {
        testCancel(true)
    }

    private fun testCancel(cancelFirst: Boolean) {
        val executor = Executors.newFixedThreadPool(2)
        try {
            // Indicates when the async task has begun.
            val taskRunning = CountDownLatch(1)
            // Indicates when the async task has been canceled.
            val taskCanceled = CountDownLatch(1)

            val call = UnaryCall<Any> { callback ->
                executor.execute {
                    taskRunning.countDown()
                    taskCanceled.await()
                    callback.invoke(
                        ResponseMessage.Failure(
                            headers = emptyMap(),
                            trailers = emptyMap(),
                            cause = ConnectException(code = Code.CANCELED),
                        ),
                    )
                }
                return@UnaryCall {
                    taskCanceled.countDown()
                }
            }
            if (cancelFirst) {
                // When we execute the task below, the call will observe
                // that it has already been canceled and immediately
                // cancel the just-started task.
                call.cancel()
            } else {
                // This will cancel the task right after it has started running.
                executor.execute {
                    taskRunning.await()
                    call.cancel()
                }
            }
            val resp = call.execute()
            assertThat(resp).isInstanceOf(ResponseMessage.Failure::class.java)
            val connEx = resp.failure { it.cause }!!
            assertThat(connEx.code).isEqualTo(Code.CANCELED)
        } finally {
            assertThat(executor.shutdownNow()).isEmpty()
        }
    }
}

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

import com.connectrpc.Code
import com.connectrpc.ConnectException
import com.connectrpc.ResponseMessage
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class UnaryCallTest {
    @Test
    fun testExecute() {
        val executor = Executors.newSingleThreadExecutor {
            val t = Thread(it)
            t.isDaemon = true
            t
        }
        try {
            val result = Object()
            val call = UnaryCall<Any> { callback ->
                val future = executor.submit {
                    try {
                        Thread.sleep(250L)
                    } catch (ex: InterruptedException) {
                        callback.invoke(
                            ResponseMessage.Failure(
                                headers = emptyMap(),
                                trailers = emptyMap(),
                                cause = ConnectException(code = Code.CANCELED, exception = ex),
                            ),
                        )
                        return@submit
                    }
                    callback.invoke(
                        ResponseMessage.Success(
                            result,
                            headers = emptyMap(),
                            trailers = emptyMap(),
                        ),
                    )
                }
                return@UnaryCall {
                    future.cancel(true)
                }
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
    fun testCancel() {
        val executor = Executors.newFixedThreadPool(2) {
            val t = Thread(it)
            t.isDaemon = true
            t
        }
        try {
            val start = System.nanoTime()
            val call = UnaryCall<Any> { callback ->
                val future = executor.submit {
                    try {
                        Thread.sleep(1_000L)
                    } catch (ex: InterruptedException) {
                        callback.invoke(
                            ResponseMessage.Failure(
                                headers = emptyMap(),
                                trailers = emptyMap(),
                                cause = ConnectException(code = Code.CANCELED, exception = ex),
                            ),
                        )
                        return@submit
                    }
                    callback.invoke(
                        ResponseMessage.Success(
                            Object(),
                            headers = emptyMap(),
                            trailers = emptyMap(),
                        ),
                    )
                }
                return@UnaryCall {
                    future.cancel(true)
                }
            }
            // Cancel should happen before normal completion
            // and should interrupt the above task.
            executor.execute {
                Thread.sleep(250L)
                call.cancel()
            }
            val resp = call.execute()
            val duration = System.nanoTime() - start

            assertThat(resp).isInstanceOf(ResponseMessage.Failure::class.java)
            val connEx = resp.failure { it.cause }!!
            assertThat(connEx.code).isEqualTo(Code.CANCELED)

            val millis = TimeUnit.MILLISECONDS.convert(duration, TimeUnit.NANOSECONDS)
            // we give extra 250ms grace period to avoid flaky failures
            assertThat(millis).isLessThan(500L)
        } finally {
            assertThat(executor.shutdownNow()).isEmpty()
        }
    }
}

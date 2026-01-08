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

package com.connectrpc.http

import kotlinx.coroutines.withContext
import okio.Buffer
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.coroutines.CoroutineContext

/**
 * Stream represents the communications for a single streaming RPC.
 * It can be used to send messages and to close the stream. Receiving
 * messages is done via callbacks provided when the stream is created.
 *
 * See HTTPClientInterface#stream.
 */
interface Stream {
    suspend fun send(buffer: Buffer): Result<Unit>

    suspend fun sendClose()

    suspend fun receiveClose()

    fun isSendClosed(): Boolean

    fun isReceiveClosed(): Boolean
}

/**
 * Creates a new stream whose implementation of sending and
 * closing is delegated to the given lambdas.
 */
@OptIn(ExperimentalAtomicApi::class)
fun Stream(
    onSend: suspend (Buffer) -> Result<Unit>,
    onSendClose: suspend () -> Unit = {},
    onReceiveClose: suspend () -> Unit = {},
): Stream {
    val isSendClosed = AtomicBoolean(false)
    val isReceiveClosed = AtomicBoolean(false)
    return object : Stream {
        override suspend fun send(buffer: Buffer): Result<Unit> {
            if (isSendClosed()) {
                return Result.failure(IllegalStateException("cannot send. underlying stream is closed"))
            }
            return try {
                onSend(buffer)
            } catch (e: Throwable) {
                Result.failure(e)
            }
        }

        override suspend fun sendClose() {
            if (isSendClosed.compareAndSet(false, true)) {
                onSendClose()
            }
        }

        override suspend fun receiveClose() {
            if (isReceiveClosed.compareAndSet(false, true)) {
                try {
                    onReceiveClose()
                } finally {
                    // When receive side is closed, the send side is
                    // implicitly closed as well.
                    // We don't use sendClose() because we don't want to
                    // invoke onSendClose() since that will try to actually
                    // half-close the HTTP stream, which will fail since
                    // closing the receive side cancels the entire thing.
                    isSendClosed.store(true)
                }
            }
        }

        override fun isSendClosed(): Boolean {
            return isSendClosed.load()
        }

        override fun isReceiveClosed(): Boolean {
            return isReceiveClosed.load()
        }
    }
}

/**
 * Returns a new stream that applies the given function to each
 * buffer when send is called. The result of that function is
 * what is passed along to the original stream.
 */
fun Stream.transform(apply: (Buffer) -> Buffer): Stream {
    val delegate = this
    return object : Stream {
        override suspend fun send(buffer: Buffer): Result<Unit> {
            return delegate.send(apply(buffer))
        }
        override suspend fun sendClose() {
            delegate.sendClose()
        }
        override suspend fun receiveClose() {
            delegate.receiveClose()
        }
        override fun isSendClosed(): Boolean {
            return delegate.isSendClosed()
        }
        override fun isReceiveClosed(): Boolean {
            return delegate.isReceiveClosed()
        }
    }
}

/**
 * Returns a new stream that dispatches suspending operations
 * (sending and closing) using the given coroutine context.
 */
fun Stream.dispatchIn(context: CoroutineContext): Stream {
    val delegate = this
    return object : Stream {
        override suspend fun send(buffer: Buffer): Result<Unit> = withContext(context) {
            delegate.send(buffer)
        }
        override suspend fun sendClose() = withContext(context) {
            delegate.sendClose()
        }
        override suspend fun receiveClose() = withContext(context) {
            delegate.receiveClose()
        }
        override fun isSendClosed(): Boolean {
            return delegate.isSendClosed()
        }
        override fun isReceiveClosed(): Boolean {
            return delegate.isReceiveClosed()
        }
    }
}

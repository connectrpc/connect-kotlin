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

package com.connectrpc.http

import com.connectrpc.StreamResult
import okio.Buffer
import java.util.concurrent.atomic.AtomicBoolean

typealias Cancelable = () -> Unit

/**
 * Interface for a client that performs underlying HTTP requests and streams with primitive types.
 */
interface HTTPClientInterface {

    /**
     * Perform a unary HTTP request.
     *
     * @param request The outbound request headers and data.
     * @param onResult The completion closure that would be called upon completion of the request.
     *
     * @return A function to cancel the underlying network call.
     */
    fun unary(request: UnaryHTTPRequest, onResult: (HTTPResponse) -> Unit): Cancelable

    /**
     * Initialize a new HTTP stream.
     *
     * @param request The request headers to use for starting the stream.
     * @param onResult The callback that would be invoked by the HTTP client when response
     *                 headers, data, and trailers are received.
     *
     * @return The created stream.
     */
    fun stream(request: HTTPRequest, duplex: Boolean, onResult: suspend (StreamResult<Buffer>) -> Unit): Stream
}

interface Stream {
    suspend fun send(buffer: Buffer): Result<Unit>

    fun sendClose()

    fun receiveClose()

    fun isSendClosed(): Boolean

    fun isReceiveClosed(): Boolean
}

fun Stream(
    onSend: suspend (Buffer) -> Result<Unit>,
    onSendClose: () -> Unit = {},
    onReceiveClose: () -> Unit = {},
): Stream {
    val isSendClosed = AtomicBoolean()
    val isReceiveClosed = AtomicBoolean()
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

        override fun sendClose() {
            if (isSendClosed.compareAndSet(false, true)) {
                onSendClose()
            }
        }

        override fun receiveClose() {
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
                    isSendClosed.set(true)
                }
            }
        }

        override fun isSendClosed(): Boolean {
            return isSendClosed.get()
        }

        override fun isReceiveClosed(): Boolean {
            return isReceiveClosed.get()
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
        override fun sendClose() {
            delegate.sendClose()
        }
        override fun receiveClose() {
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

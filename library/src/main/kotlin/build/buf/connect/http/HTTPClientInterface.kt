// Copyright 2022-2023 Buf Technologies, Inc.
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

package build.buf.connect.http

import build.buf.connect.StreamResult
import okio.Buffer
import java.util.concurrent.atomic.AtomicReference

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
    fun unary(request: HTTPRequest, onResult: (HTTPResponse) -> Unit): Cancelable

    /**
     * Initialize a new HTTP stream.
     *
     * @param request The request headers to use for starting the stream.
     * @param onResult The callback that would be invoked by the HTTP client when response
     *                 headers, data, and trailers are received.
     *
     * @return The created stream.
     */
    fun stream(request: HTTPRequest, onResult: suspend (StreamResult<Buffer>) -> Unit): Stream
}

class Stream(
    private val onSend: (Buffer) -> Unit,
    private val onSendClose: () -> Unit = {},
    private val onReceiveClose: () -> Unit = {}
) {
    private val isSendClosed = AtomicReference(false)
    private val isReceiveClosed = AtomicReference(false)

    fun send(buffer: Buffer): Result<Unit> {
        if (isClosed()) {
            return Result.failure(IllegalStateException("cannot send. underlying stream is closed"))
        }
        return try {
            onSend(buffer)
            Result.success(Unit)
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    fun sendClose() {
        if (!isSendClosed.getAndSet(true)) {
            onSendClose()
        }
    }

    fun receiveClose() {
        if (!isReceiveClosed.getAndSet(true)) {
            onReceiveClose()
        }
    }

    fun isClosed(): Boolean {
        return isSendClosed() && isReceiveClosed()
    }

    fun isSendClosed(): Boolean {
        return isSendClosed.get()
    }

    fun isReceiveClosed(): Boolean {
        return isReceiveClosed.get()
    }
}

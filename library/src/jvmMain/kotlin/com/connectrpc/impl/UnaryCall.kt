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

import com.connectrpc.ResponseMessage
import com.connectrpc.UnaryBlockingCall
import com.connectrpc.http.Cancelable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * Callback that handles asynchronous response.
 */
internal typealias ResponseCallback<T> = (ResponseMessage<T>) -> Unit

/**
 * Represents a cancelable asynchronous operation. When the function
 * is invoked, the operation is initiated. When that operation completes
 * it MUST invoke the callback, even when canceled. The value returned
 * from the function can be called to abort the operation and have it
 * return early.
 */
internal typealias AsyncOperation<T> = (callback: ResponseCallback<T>) -> Cancelable

/**
 * Concrete implementation of [UnaryBlockingCall] which transforms
 * the given async operation into a synchronous/blocking one.
 */
internal class UnaryCall<Output>(
    private val block: AsyncOperation<Output>,
) : UnaryBlockingCall<Output> {
    private val executed = AtomicBoolean()

    /**
     * initialized to null and then replaced with non-null
     * function when [execute] or [cancel] is called.
     */
    private var cancelFunc = AtomicReference<Cancelable>()

    /**
     * Execute the underlying operation and block until it completes.
     */
    override fun execute(): ResponseMessage<Output> {
        check(executed.compareAndSet(false, true)) { "already executed" }

        val resultReady = CountDownLatch(1)
        val result = AtomicReference<ResponseMessage<Output>>()
        val cancelFn = block { responseMessage ->
            result.set(responseMessage)
            resultReady.countDown()
        }

        if (!cancelFunc.compareAndSet(null, cancelFn)) {
            // concurrently cancelled before we could set the
            // cancel function, so we need to cancel what we
            // just started
            cancelFn()
        }
        resultReady.await()
        return result.get()
    }

    /**
     * Cancel the underlying request.
     */
    override fun cancel() {
        val cancelFn = cancelFunc.getAndSet {} // set to (non-null) no-op
        if (cancelFn != null) {
            cancelFn()
        }
    }
}

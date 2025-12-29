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

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.time.Duration

/**
 * Represents the timeout state for an RPC.
 */
@OptIn(ExperimentalAtomicApi::class)
class Timeout private constructor(
    private val timeoutAction: Cancelable,
) {
    private val done = AtomicBoolean(false)

    @Volatile private var triggered: Boolean = false
    private var onCancel: Cancelable? = null

    /** Returns true if this timeout has lapsed and the associated RPC canceled. */
    val timedOut: Boolean
        get() = triggered

    /**
     * Cancels the timeout. Should only be called when the RPC completes before the
     * timeout elapses. Returns true if the timeout was canceled or false if either
     * it was already previously canceled or has already timed out. The `timedOut`
     * property can be queried to distinguish between these two possibilities.
     */
    fun cancel(): Boolean {
        if (done.compareAndSet(false, true)) {
            onCancel?.invoke()
            return true
        }
        return false
    }

    private fun trigger() {
        if (done.compareAndSet(false, true)) {
            triggered = true
            timeoutAction()
        }
    }

    /** Schedules timeouts for RPCs. */
    interface Scheduler {
        /**
         * Schedules a timeout that should invoke the given action to cancel
         * an RPC after the given delay.
         */

        fun scheduleTimeout(delay: Duration, action: Cancelable): Timeout
    }

    companion object {
        /**
         * A default implementation using coroutines for scheduling.
         */
        val DEFAULT_SCHEDULER = object : Scheduler {
            private val scope = CoroutineScope(Dispatchers.IO)

            override fun scheduleTimeout(delay: Duration, action: Cancelable): Timeout {
                val timeout = Timeout(action)
                val job = scope.launch {
                    delay(delay)
                    timeout.trigger()
                }
                timeout.onCancel = { job.cancel() }
                return timeout
            }
        }
    }
}

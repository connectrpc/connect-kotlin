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

import kotlinx.coroutines.delay
import java.util.Timer
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.timerTask
import kotlin.time.Duration

/**
 * Represents the timeout state for an RPC.
 */
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
         * A default implementation that a Timer backed by a single daemon thread.
         * The thread isn't started until the first cancelation is scheduled.
         */
        val DEFAULT_SCHEDULER = object : Scheduler {
            override fun scheduleTimeout(delay: Duration, action: Cancelable): Timeout {
                val timeout = Timeout(action)
                val task = timerTask { timeout.trigger() }
                timer.value.schedule(task, delay.inWholeMilliseconds)
                timeout.onCancel = { task.cancel() }
                return timeout
            }
        }

        private val timer = lazy { Timer(Scheduler::class.qualifiedName, true) }
    }
}

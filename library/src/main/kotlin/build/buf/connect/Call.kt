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

package build.buf.connect

import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference

/**
 * A [Call] contains the way to make a blocking RPC call and cancelling the RPC.
 */
class Call<Output> {
    private var executable: ((ResponseMessage<Output>) -> Unit) -> Unit = { }
    private var cancel: () -> Unit = { }

    /**
     * Execute the underlying request.
     */
    fun execute(): ResponseMessage<Output> {
        val countDownLatch = CountDownLatch(1)
        val reference = AtomicReference<ResponseMessage<Output>>();
        executable { responseMessage ->
            reference.set(responseMessage)
            countDownLatch.countDown()
        }
        countDownLatch.await()
        return reference.get()
    }

    /**
     * Cancel the underlying request.
     */
    fun cancel() {
        cancel()
    }

    internal fun setCancel(cancel: () -> Unit) {
        this.cancel = cancel;
    }

    internal fun setExecute(executable: ((ResponseMessage<Output>) -> Unit) -> Unit) {
        this.executable = executable
    }
}

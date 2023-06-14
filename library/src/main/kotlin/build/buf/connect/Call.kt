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

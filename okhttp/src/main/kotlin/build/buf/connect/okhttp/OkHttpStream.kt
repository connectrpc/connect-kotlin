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

package build.buf.connect.okhttp

import build.buf.connect.Code
import build.buf.connect.ConnectError
import build.buf.connect.StreamResult
import build.buf.connect.http.HTTPRequest
import build.buf.connect.http.Stream
import kotlinx.coroutines.runBlocking
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okhttp3.internal.http2.StreamResetException
import okio.Buffer
import okio.BufferedSink
import okio.BufferedSource
import okio.Pipe
import okio.buffer
import java.io.IOException
import java.io.InterruptedIOException
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Extension function for OkHttpClient to initialize a stream.
 *
 * This is responsible for creating a bidirectional stream with OkHttp.
 */
internal fun OkHttpClient.initializeStream(
    request: HTTPRequest,
    onResult: suspend (StreamResult<Buffer>) -> Unit
): Stream {
    val isClosed = AtomicBoolean(false)
    val duplexRequestBody = PipeDuplexRequestBody(request.contentType.toMediaType())
    val builder = Request.Builder()
        .url(request.url)
        .method("POST", duplexRequestBody)
    for (entry in request.headers) {
        for (values in entry.value) {
            builder.addHeader(entry.key, values)
        }
    }
    val callRequest = builder.build()
    val call = newCall(callRequest)
    call.enqueue(ResponseCallback(onResult, isClosed))
    return Stream(
        onSend = { buffer ->
            if (!isClosed.get()) {
                duplexRequestBody.forConsume(buffer)
            }
        },
        onClose = {
            try {
                isClosed.set(true)
                call.cancel()
                duplexRequestBody.close()
            } catch (_: Throwable) {
                // No-op
            }
        }
    )
}

private class ResponseCallback(
    private val onResult: suspend (StreamResult<Buffer>) -> Unit,
    private val isClosed: AtomicBoolean
) : Callback {
    override fun onFailure(call: Call, e: IOException) {
        runBlocking {
            if (e is InterruptedIOException) {
                if (e.message == "timeout") {
                    val error = ConnectError(code = Code.DEADLINE_EXCEEDED)
                    onResult(StreamResult.Complete(Code.DEADLINE_EXCEEDED, error = error))
                    return@runBlocking
                }
            }
            onResult(StreamResult.Complete(Code.UNKNOWN, error = e))
        }
    }

    override fun onResponse(call: Call, response: Response) {
        val code = Code.fromHTTPStatus(response.code)
        runBlocking {
            onResult(StreamResult.Headers(headers = response.headers.toLowerCaseKeysMultiMap()))
            if (code != Code.OK) {
                // TODO: This is not quite exercised yet. Validate if this is exercised in another test case.
                val finalResult = StreamResult.Complete<Buffer>(
                    code = code,
                    trailers = response.safeTrailers() ?: emptyMap(),
                    error = ConnectError(code = code)
                )
                onResult(finalResult)
                return@runBlocking
            }
            response.use { resp ->
                resp.body!!.source().use { sourceBuffer ->
                    var exception: Throwable? = null
                    try {
                        while (!sourceBuffer.safeExhausted() && !isClosed.get()) {
                            val buffer = readStream(sourceBuffer)
                            val streamResult = StreamResult.Message(
                                message = buffer
                            )
                            onResult(streamResult)
                        }
                    } catch (e: Exception) {
                        exception = e
                    } finally {
                        // If trailers are not yet communicated.
                        // This is the final chance to notify trailers to the consumer.
                        val finalResult = StreamResult.Complete<Buffer>(
                            code = code,
                            trailers = response.safeTrailers() ?: emptyMap(),
                            error = exception
                        )
                        onResult(finalResult)
                    }
                }
            }
        }
    }

    private fun BufferedSource.safeExhausted(): Boolean {
        return try {
            exhausted()
        } catch (e: StreamResetException) {
            true
        }
    }

    private fun Response.safeTrailers(): Map<String, List<String>>? {
        return try {
            if (body?.source()?.safeExhausted() == false) {
                // Assuming this means that trailers are not available.
                // Returning null to signal trailers are "missing".
                return null
            }
            trailers().toLowerCaseKeysMultiMap()
        } catch (_: Throwable) {
            // Something went terribly wrong.
            emptyMap()
        }
    }

    /**
     * Helps with reading and framing OkHttp responses into Buffers.
     *
     * The main assumption made here is that stream responses are enveloped
     * and the second part of the envelope header is the message length.
     *
     * This does not do anything related to the first part of
     * the envelope header.
     */
    private fun readStream(bufferSource: BufferedSource): Buffer {
        val compressionHeader = bufferSource.readByte()
        val messageLength = bufferSource.readInt()
        val result = Buffer()
        result.writeByte(compressionHeader.toInt())
        result.writeInt(messageLength)
        result.write(bufferSource, messageLength.toLong())
        return result
    }
}

internal class PipeDuplexRequestBody(
    private val contentType: MediaType?,
    pipeMaxBufferSize: Long = 1024 * 1024
) : RequestBody() {
    private val pipe = Pipe(pipeMaxBufferSize)

    private val bufferedSink by lazy { pipe.sink.buffer() }

    fun forConsume(buffer: Buffer) {
        try {
            if (bufferedSink.isOpen) {
                bufferedSink.writeAll(buffer)
                bufferedSink.flush()
            }
        } catch (e: Throwable) {
            close()
        }
    }

    override fun contentType() = contentType

    override fun writeTo(sink: BufferedSink) {
        pipe.fold(sink)
    }

    override fun isDuplex() = true

    fun close() {
        try {
            bufferedSink.close()
        } catch (_: Throwable) {
            // No-op
        }
    }
}

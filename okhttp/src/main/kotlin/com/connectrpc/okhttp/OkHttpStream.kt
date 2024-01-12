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

package com.connectrpc.okhttp

import com.connectrpc.Code
import com.connectrpc.ConnectException
import com.connectrpc.StreamResult
import com.connectrpc.http.HTTPRequest
import com.connectrpc.http.Stream
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
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Extension function for OkHttpClient to initialize a stream.
 *
 * This is responsible for creating a bidirectional stream with OkHttp.
 */
internal fun OkHttpClient.initializeStream(
    method: String,
    request: HTTPRequest,
    duplex: Boolean,
    onResult: suspend (StreamResult<Buffer>) -> Unit,
): Stream {
    val isSendClosed = AtomicBoolean(false)
    val isReceiveClosed = AtomicBoolean(false)
    val duplexRequestBody = PipeRequestBody(duplex, request.contentType.toMediaType())
    val builder = Request.Builder()
        .url(request.url)
        .method(method, duplexRequestBody)
    for (entry in request.headers) {
        for (values in entry.value) {
            builder.addHeader(entry.key, values)
        }
    }
    val callRequest = builder.build()
    val call = newCall(callRequest)
    call.enqueue(ResponseCallback(onResult))
    return Stream(
        onSend = { buffer ->
            if (!isSendClosed.get()) {
                duplexRequestBody.forConsume(buffer)
            }
        },
        onSendClose = {
            isSendClosed.set(true)
            duplexRequestBody.close()
        },
        onReceiveClose = {
            isReceiveClosed.set(true)
            call.cancel()
            // cancelling implicitly closes send-side, too
            isSendClosed.set(true)
        },
    )
}

private class ResponseCallback(
    private val onResult: suspend (StreamResult<Buffer>) -> Unit,
) : Callback {
    override fun onFailure(call: Call, e: IOException) {
        runBlocking {
            onResult(StreamResult.Complete(codeFromException(call.isCanceled(), e), cause = e))
        }
    }

    override fun onResponse(call: Call, response: Response) {
        val code = Code.fromHTTPStatus(response.code)
        runBlocking {
            val headers = response.headers.toLowerCaseKeysMultiMap()
            onResult(StreamResult.Headers(headers = headers))
            if (code != Code.OK) {
                // TODO: This is not quite exercised yet. Validate if this is exercised in another test case.
                val finalResult = StreamResult.Complete<Buffer>(
                    code = code,
                    trailers = response.safeTrailers() ?: emptyMap(),
                    cause = ConnectException(code = code),
                )
                onResult(finalResult)
                return@runBlocking
            }
            response.use { resp ->
                resp.body!!.source().use { sourceBuffer ->
                    var exception: Exception? = null
                    try {
                        while (!sourceBuffer.exhausted()) {
                            val buffer = readStream(sourceBuffer)
                            val streamResult = StreamResult.Message(
                                message = buffer,
                            )
                            onResult(streamResult)
                        }
                    } catch (e: Exception) {
                        exception = e
                    } finally {
                        // If trailers are not yet communicated.
                        // This is the final chance to notify trailers to the consumer.
                        val finalResult = StreamResult.Complete<Buffer>(
                            code = if (exception != null) codeFromException(call.isCanceled(), exception) else code,
                            trailers = response.safeTrailers() ?: emptyMap(),
                            cause = exception,
                        )
                        onResult(finalResult)
                    }
                }
            }
        }
    }

    private fun Response.safeTrailers(): Map<String, List<String>>? {
        try {
            if (body?.source()?.exhausted() == false) {
                // Assuming this means that trailers are not available.
                // Returning null to signal trailers are "missing".
                return null
            }
        } catch (e: Exception) {
            return null
        }

        return try {
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

internal class PipeRequestBody(
    private val duplex: Boolean,
    private val contentType: MediaType?,
    pipeMaxBufferSize: Long = 1024 * 1024,
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
            throw e
        }
    }

    override fun contentType() = contentType

    override fun writeTo(sink: BufferedSink) {
        pipe.fold(sink)
    }

    override fun isDuplex() = duplex

    override fun isOneShot() = true

    fun close() {
        try {
            bufferedSink.close()
        } catch (_: Throwable) {
            // No-op
        }
    }
}

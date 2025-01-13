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

package com.connectrpc.okhttp

import com.connectrpc.Code
import com.connectrpc.ConnectException
import com.connectrpc.StreamResult
import com.connectrpc.asConnectException
import com.connectrpc.http.HTTPMethod
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
import okio.Buffer
import okio.BufferedSink
import okio.BufferedSource
import okio.Pipe
import java.io.IOException
import java.util.concurrent.CountDownLatch

/**
 * Extension function for OkHttpClient to initialize a stream.
 *
 * This is responsible for creating a bidirectional stream with OkHttp.
 */
internal fun OkHttpClient.initializeStream(
    request: HTTPRequest,
    duplex: Boolean,
    onResult: suspend (StreamResult<Buffer>) -> Unit,
): Stream {
    val requestBody = PipeRequestBody(duplex, request.contentType.toMediaType())
    val builder = Request.Builder()
        .url(request.url)
        .method(HTTPMethod.POST.string, requestBody) // streams are always POSTs
    for (entry in request.headers) {
        for (values in entry.value) {
            builder.addHeader(entry.key, values)
        }
    }
    val callRequest = builder.build()
    val call = newCall(callRequest)
    // For non-duplex bodies, the request is complete when the
    // response arrives.
    val whenDone = if (!duplex) requestBody::close else { -> }
    call.enqueue(ResponseCallback(onResult, whenDone))
    return Stream(
        onSend = { buffer ->
            try {
                requestBody.write(buffer)
                Result.success(Unit)
            } catch (ex: Throwable) {
                Result.failure(ex)
            }
        },
        onSendClose = {
            requestBody.close()
        },
        onReceiveClose = {
            call.cancel()
            whenDone()
        },
    )
}

private class ResponseCallback(
    private val onResult: suspend (StreamResult<Buffer>) -> Unit,
    private val whenDone: () -> Unit,
) : Callback {
    override fun onFailure(call: Call, e: IOException) {
        whenDone()
        runBlocking {
            onResult(
                StreamResult.Complete(
                    cause = ConnectException(
                        code = codeFromException(call.isCanceled(), e),
                        exception = e,
                    ),
                ),
            )
        }
    }

    override fun onResponse(call: Call, response: Response) {
        whenDone()
        val httpStatus = response.originalCode()
        runBlocking {
            val headers = response.headers.toLowerCaseKeysMultiMap()
            onResult(StreamResult.Headers(headers = headers))
            if (httpStatus != 200) {
                // TODO: This is not quite exercised yet. Validate if this is exercised in another test case.
                val finalResult = StreamResult.Complete<Buffer>(
                    trailers = response.safeTrailers(),
                    cause = ConnectException(
                        code = Code.fromHTTPStatus(httpStatus),
                        message = "unexpected HTTP status: $httpStatus ${response.originalMessage()}",
                        metadata = headers,
                    ),
                )
                onResult(finalResult)
                return@runBlocking
            }
            response.use { resp ->
                resp.body!!.source().use { sourceBuffer ->
                    var connEx: ConnectException? = null
                    try {
                        while (!sourceBuffer.exhausted()) {
                            val buffer = readStreamElement(sourceBuffer)
                            val streamResult = StreamResult.Message(
                                message = buffer,
                            )
                            onResult(streamResult)
                        }
                    } catch (ex: Exception) {
                        connEx = asConnectException(ex, codeFromException(call.isCanceled(), ex))
                    } finally {
                        // If trailers are not yet communicated.
                        // This is the final chance to notify trailers to the consumer.
                        val finalResult = StreamResult.Complete<Buffer>(
                            trailers = response.safeTrailers(),
                            cause = connEx,
                        )
                        onResult(finalResult)
                    }
                }
            }
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
    private fun readStreamElement(bufferSource: BufferedSource): Buffer {
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

    /**
     * Latch that signals when the pipe's sink is closed.
     */
    private val closed = CountDownLatch(1)

    fun write(buffer: Buffer) {
        try {
            pipe.sink.write(buffer, buffer.size)
            pipe.sink.flush()
        } catch (e: Throwable) {
            close()
            throw e
        }
    }

    override fun contentType() = contentType

    override fun writeTo(sink: BufferedSink) {
        pipe.fold(sink)
        if (!duplex) {
            // For non-duplex request bodies, okhttp3
            // expects this method to return only when
            // the request body is complete.
            closed.await()
        }
    }

    override fun isDuplex() = duplex

    override fun isOneShot() = true

    fun close() {
        try {
            pipe.sink.close()
        } catch (_: Throwable) {
            // No-op
        } finally {
            closed.countDown()
        }
    }
}

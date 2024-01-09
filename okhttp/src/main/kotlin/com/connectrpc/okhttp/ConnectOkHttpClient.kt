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
import com.connectrpc.http.Cancelable
import com.connectrpc.http.HTTPClientInterface
import com.connectrpc.http.HTTPRequest
import com.connectrpc.http.HTTPResponse
import com.connectrpc.http.Stream
import com.connectrpc.http.TracingInfo
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.internal.http.HttpMethod
import okio.Buffer
import java.io.IOException
import java.io.InterruptedIOException
import java.net.SocketTimeoutException

/**
 * The OkHttp implementation of HTTPClientInterface.
 */
class ConnectOkHttpClient @JvmOverloads constructor(
    private val unaryClient: OkHttpClient = OkHttpClient(),
    private val streamClient: OkHttpClient = unaryClient,
) : HTTPClientInterface {

    override fun unary(request: HTTPRequest, onResult: (HTTPResponse) -> Unit): Cancelable {
        val builder = okhttp3.Request.Builder()
        for (entry in request.headers) {
            for (values in entry.value) {
                builder.addHeader(entry.key, values)
            }
        }
        val content = request.message ?: ByteArray(0)
        val method = request.httpMethod
        val requestBody = if (HttpMethod.requiresRequestBody(method)) content.toRequestBody(request.contentType.toMediaType()) else null
        val callRequest = builder
            .url(request.url)
            .method(method, requestBody)
            .build()
        val newCall = unaryClient.newCall(callRequest)
        val cancelable = {
            newCall.cancel()
        }
        try {
            newCall.enqueue(
                object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        val code = codeFromIOException(e)
                        onResult(
                            HTTPResponse(
                                code = code,
                                headers = emptyMap(),
                                message = Buffer(),
                                trailers = emptyMap(),
                                cause = ConnectException(
                                    code,
                                    message = e.message,
                                    exception = e,
                                ),
                                tracingInfo = null,
                            ),
                        )
                    }

                    override fun onResponse(call: Call, response: Response) {
                        // Unary requests will need to read the entire body to access trailers.
                        val responseBuffer = response.body?.source()?.use { bufferedSource ->
                            val buffer = Buffer()
                            buffer.writeAll(bufferedSource)
                            buffer
                        }
                        onResult(
                            HTTPResponse(
                                code = Code.fromHTTPStatus(response.code),
                                headers = response.headers.toLowerCaseKeysMultiMap(),
                                message = responseBuffer ?: Buffer(),
                                trailers = response.trailers().toLowerCaseKeysMultiMap(),
                                tracingInfo = TracingInfo(response.code),
                            ),
                        )
                    }
                },
            )
        } catch (e: Throwable) {
            onResult(
                HTTPResponse(
                    code = Code.UNKNOWN,
                    headers = emptyMap(),
                    message = Buffer(),
                    trailers = emptyMap(),
                    cause = ConnectException(
                        Code.UNKNOWN,
                        message = e.message,
                        exception = e,
                    ),
                    tracingInfo = null,
                ),
            )
        }
        return cancelable
    }

    override fun stream(
        request: HTTPRequest,
        onResult: suspend (StreamResult<Buffer>) -> Unit,
    ): Stream {
        return streamClient.initializeStream(request.httpMethod, request, onResult)
    }
}

internal fun Headers.toLowerCaseKeysMultiMap(): Map<String, List<String>> {
    return this.asSequence().groupBy(
        { it.first.lowercase() },
        { it.second },
    )
}

internal fun codeFromIOException(e: IOException): Code {
    return if ((e is InterruptedIOException && e.message == "timeout") ||
        e is SocketTimeoutException
    ) {
        Code.DEADLINE_EXCEEDED
    } else if (e.message?.lowercase() == "canceled") {
        // TODO: Figure out what, if anything, actually throws an exception
        //       with this message. It seems more likely that a JVM or
        //       Kotlin coroutine exception would spell it with two Ls.
        Code.CANCELED
    } else {
        Code.UNKNOWN
    }
}

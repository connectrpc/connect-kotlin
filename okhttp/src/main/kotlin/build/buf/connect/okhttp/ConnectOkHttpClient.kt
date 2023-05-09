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
import build.buf.connect.http.Cancelable
import build.buf.connect.http.HTTPClientInterface
import build.buf.connect.http.HTTPRequest
import build.buf.connect.http.HTTPResponse
import build.buf.connect.http.Stream
import build.buf.connect.http.TracingInfo
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.Buffer
import java.io.IOException

private const val POST_METHOD = "POST"
private const val GET_METHOD = "GET"

/**
 * The OkHttp implementation of HTTPClientInterface.
 */
class ConnectOkHttpClient(
    val client: OkHttpClient = OkHttpClient()
) : HTTPClientInterface {

    override fun unary(request: HTTPRequest, onResult: (HTTPResponse) -> Unit): Cancelable {
        return unary(POST_METHOD, request, onResult)
    }

    override fun unary(method: String, request: HTTPRequest, onResult: (HTTPResponse) -> Unit): Cancelable {
        val builder = okhttp3.Request.Builder()
        for (entry in request.headers) {
            for (values in entry.value) {
                builder.addHeader(entry.key, values)
            }
        }
        val content = request.message ?: ByteArray(0)
        val requestBody = if (method == GET_METHOD) null else content.toRequestBody(request.contentType.toMediaType())
        val callRequest = builder
            .url(request.url)
            .method(method, requestBody)
            .build()
        val newCall = client.newCall(callRequest)
        val cancelable = {
            newCall.cancel()
        }
        try {
            newCall.enqueue(
                object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        val code = if (e.message?.lowercase() == "canceled") {
                            Code.CANCELED
                        } else {
                            Code.UNKNOWN
                        }
                        onResult(
                            HTTPResponse(
                                code = code,
                                headers = emptyMap(),
                                message = Buffer(),
                                trailers = emptyMap(),
                                error = ConnectError(
                                    code,
                                    message = e.message,
                                    exception = e
                                ),
                                tracingInfo = null
                            )
                        )
                    }

                    override fun onResponse(call: Call, response: Response) {
                        // Unary requests will need to read the entire body to access trailers.
                        val responseBuffer = response.body?.source()?.use { bufferedSource ->
                            val buffer = Buffer()
                            buffer.writeAll(bufferedSource)
                            buffer
                        }
                        val httpResponse = HTTPResponse(
                            code = Code.fromHTTPStatus(response.code),
                            headers = response.headers.toLowerCaseKeysMultiMap(),
                            message = responseBuffer ?: Buffer(),
                            trailers = response.trailers().toLowerCaseKeysMultiMap(),
                            tracingInfo = TracingInfo(response.code)
                        )
                        println(httpResponse)
                        onResult(
                            httpResponse
                        )
                    }
                }
            )
        } catch (e: Throwable) {
            onResult(
                HTTPResponse(
                    code = Code.UNKNOWN,
                    headers = emptyMap(),
                    message = Buffer(),
                    trailers = emptyMap(),
                    error = ConnectError(
                        Code.UNKNOWN,
                        message = e.message,
                        exception = e
                    ),
                    tracingInfo = null
                )
            )
        }
        return cancelable
    }

    override fun stream(request: HTTPRequest, onResult: suspend (StreamResult<Buffer>) -> Unit): Stream {
        return stream(POST_METHOD, request, onResult)
    }

    override fun stream(
        method: String,
        request: HTTPRequest,
        onResult: suspend (StreamResult<Buffer>) -> Unit
    ): Stream {
        return client.initializeStream(method, request, onResult)
    }
}

internal fun Headers.toLowerCaseKeysMultiMap(): Map<String, List<String>> {
    return toMultimap()
        .map { (key, value) ->
            key.lowercase() to value
        }
        .toMap()
}

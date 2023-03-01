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

package build.buf.connect.apache

import build.buf.connect.Code
import build.buf.connect.ConnectError
import build.buf.connect.StreamResult
import build.buf.connect.http.Cancelable
import build.buf.connect.http.HTTPClientInterface
import build.buf.connect.http.HTTPRequest
import build.buf.connect.http.HTTPResponse
import build.buf.connect.http.Stream
import build.buf.connect.http.TracingInfo
import okio.Buffer
import org.apache.http.Header
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.ByteArrayEntity
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.io.ChunkedInputStream
import java.lang.UnsupportedOperationException
import java.util.Locale

private const val CONTENT_TYPE = "content-type"

class ConnectApacheHttpClient(
    private val httpClient: CloseableHttpClient
) : HTTPClientInterface {
    override fun unary(request: HTTPRequest, onResult: (HTTPResponse) -> Unit): Cancelable {
        val httpPost = HttpPost(request.url.toURI())
        for (entry in request.headers) {
            for (value in entry.value) {
                httpPost.addHeader(entry.key, value)
            }
        }
        httpPost.addHeader(CONTENT_TYPE, request.contentType)
        val content = request.message ?: ByteArray(0)
        httpPost.entity = ByteArrayEntity(content)
        var cancelable = { }
        try {
            val response = httpClient.execute(httpPost)
            cancelable = {
                response.close()
            }
            val entityContent = response.entity.content
            val responseBuffer = Buffer().readFrom(entityContent)
            val trailers = if (response.entity.isChunked) {
                val chunkedInputStream = entityContent as ChunkedInputStream
                chunkedInputStream.footers
            } else {
                emptyArray()
            }
            entityContent.close()
            val headers = mutableMapOf<String, List<String>>()
            headers.putAll(toLowerCaseKeysMultiMap(response.allHeaders))
            val contentEncodingHeader = response.entity.contentEncoding
            if (contentEncodingHeader != null) {
                headers.put(contentEncodingHeader.name.lowercase(Locale.US), listOf(contentEncodingHeader.value))
            }
            onResult(
                HTTPResponse(
                    code = Code.fromHTTPStatus(response.statusLine.statusCode),
                    headers = headers,
                    message = responseBuffer,
                    trailers = toLowerCaseKeysMultiMap(trailers),
                    tracingInfo = TracingInfo(response.statusLine.statusCode)
                )
            )
        } catch (e: Throwable) {
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
        return cancelable
    }

    override fun stream(request: HTTPRequest, onResult: suspend (StreamResult<Buffer>) -> Unit): Stream {
        throw UnsupportedOperationException("Streaming not yet supported in Apache client.")
    }

    private fun toLowerCaseKeysMultiMap(headers: Array<Header>): Map<String, List<String>> {
        val result = mutableMapOf<String, MutableList<String>>()
        for (header in headers) {
            val key = header.name.lowercase(Locale.US)
            val headerValue = result[key] ?: mutableListOf()
            headerValue.add(header.value)
            result[key] = headerValue
        }
        return result
    }
}

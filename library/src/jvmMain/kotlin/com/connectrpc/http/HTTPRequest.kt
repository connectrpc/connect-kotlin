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

import com.connectrpc.Headers
import com.connectrpc.MethodSpec
import io.ktor.http.Url
import okio.Buffer
import kotlin.time.Duration

enum class HTTPMethod(
    val string: String,
) {
    GET("GET"),
    POST("POST"),
}

/**
 * HTTP request used to initiate RPCs.
 */
open class HTTPRequest internal constructor(
    // The URL for the request.
    val url: Url,
    // Value to assign to the `content-type` header.
    val contentType: String,
    // The optional timeout for this request.
    val timeout: Duration?,
    // Additional outbound headers for the request.
    val headers: Headers,
    // The method spec associated with the request.
    val methodSpec: MethodSpec<*, *>,
)

/**
 * Clones the [HTTPRequest] with override values.
 *
 * Intended to make mutations for [HTTPRequest] safe for
 * [com.connectrpc.Interceptor] implementation.
 */
fun HTTPRequest.clone(
    // The URL for the request.
    url: Url = this.url,
    // Value to assign to the `content-type` header.
    contentType: String = this.contentType,
    // The optional timeout for this request.
    timeout: Duration? = this.timeout,
    // Additional outbound headers for the request.
    headers: Headers = this.headers,
    // The method spec associated with the request.
    methodSpec: MethodSpec<*, *> = this.methodSpec,
): HTTPRequest {
    return HTTPRequest(
        url,
        contentType,
        timeout,
        headers,
        methodSpec,
    )
}

/**
 * HTTP request used to initiate unary RPCs. In addition
 * to RPC metadata, this also includes the request data.
 */
class UnaryHTTPRequest(
    // The URL for the request.
    url: Url,
    // Value to assign to the `content-type` header.
    contentType: String,
    // The optional timeout for this request.
    timeout: Duration?,
    // Additional outbound headers for the request.
    headers: Headers,
    // The method spec associated with the request.
    methodSpec: MethodSpec<*, *>,
    // Body data for the request.
    val message: Buffer,
    // HTTP method to use with the request.
    // Almost always POST, but side effect free unary RPCs may be made with GET.
    val httpMethod: HTTPMethod = HTTPMethod.POST,
) : HTTPRequest(url, contentType, timeout, headers, methodSpec)

fun UnaryHTTPRequest.clone(
    // The URL for the request.
    url: Url = this.url,
    // Value to assign to the `content-type` header.
    contentType: String = this.contentType,
    // The optional timeout for this request.
    timeout: Duration? = this.timeout,
    // Additional outbound headers for the request.
    headers: Headers = this.headers,
    // The method spec associated with the request.
    methodSpec: MethodSpec<*, *> = this.methodSpec,
    // Body data for the request.
    message: Buffer = this.message,
    // The HTTP method to use with the request.
    httpMethod: HTTPMethod = this.httpMethod,
): UnaryHTTPRequest {
    return UnaryHTTPRequest(
        url,
        contentType,
        timeout,
        headers,
        methodSpec,
        message,
        httpMethod,
    )
}

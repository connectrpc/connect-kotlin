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

import com.connectrpc.ConnectException
import com.connectrpc.Headers
import com.connectrpc.Trailers
import okio.BufferedSource

/**
 * Unary HTTP response received from the server.
 */
class HTTPResponse(
    // The underlying http status code. If null,
    // no response was ever received on the network
    // and cause must be non-null.
    val status: Int?,
    // Response headers specified by the server.
    val headers: Headers,
    // Body data provided by the server.
    val message: BufferedSource,
    // Trailers provided by the server.
    val trailers: Trailers,
    // The accompanying exception, if the request failed.
    val cause: ConnectException? = null,
)

/**
 * Clones the [HTTPResponse] with override values.
 *
 * Intended to make mutations for [HTTPResponse] safe for
 * [com.connectrpc.Interceptor] implementation.
 */
fun HTTPResponse.clone(
    // The status code of the response.
    status: Int? = this.status,
    // Response headers specified by the server.
    headers: Headers = this.headers,
    // Body data provided by the server.
    message: BufferedSource = this.message,
    // Trailers provided by the server.
    trailers: Trailers = this.trailers,
    // The accompanying error, if the request failed.
    cause: ConnectException? = this.cause,
): HTTPResponse {
    return HTTPResponse(
        status,
        headers,
        message,
        trailers,
        cause,
    )
}

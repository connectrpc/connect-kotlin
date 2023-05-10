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

package build.buf.connect.http

import build.buf.connect.Headers
import build.buf.connect.MethodSpec
import java.net.URL

/**
 * HTTP request used for sending primitive data to the server.
 */
class HTTPRequest internal constructor(
    // The URL for the request.
    val url: URL,
    // Value to assign to the `content-type` header.
    val contentType: String,
    // Additional outbound headers for the request.
    val headers: Headers,
    // Body data to send with the request.
    val message: ByteArray? = null,
    // The method spec associated with the request.
    val methodSpec: MethodSpec<*, *>
) {
    /**
     * Clones the [HTTPRequest] with override values.
     *
     * Intended to make mutations for [HTTPRequest] safe for
     * [build.buf.connect.Interceptor] implementation.
     */
    fun clone(
        // The URL for the request.
        url: URL = this.url,
        // Value to assign to the `content-type` header.
        contentType: String = this.contentType,
        // Additional outbound headers for the request.
        headers: Headers = this.headers,
        // Body data to send with the request.
        message: ByteArray? = this.message,
        // The method spec associated with the request.
        methodSpec: MethodSpec<*, *> = this.methodSpec
    ): HTTPRequest {
        return HTTPRequest(
            url,
            contentType,
            headers,
            message,
            methodSpec
        )
    }
}

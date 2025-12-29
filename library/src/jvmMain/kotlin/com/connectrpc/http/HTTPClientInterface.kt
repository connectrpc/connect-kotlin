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

import com.connectrpc.StreamResult
import okio.Buffer

/** A function that cancels an operation when called. */
typealias Cancelable = () -> Unit

/**
 * Interface for a client that performs underlying HTTP requests and streams with primitive types.
 */
interface HTTPClientInterface {

    /**
     * Perform a unary HTTP request.
     *
     * @param request The outbound request headers and data.
     * @param onResult The completion closure that would be called upon completion of the request.
     *
     * @return A function to cancel the underlying network call.
     */
    fun unary(request: UnaryHTTPRequest, onResult: (HTTPResponse) -> Unit): Cancelable

    /**
     * Initialize a new HTTP stream.
     *
     * @param request The request headers to use for starting the stream.
     * @param onResult The callback that would be invoked by the HTTP client when response
     *                 headers, data, and trailers are received.
     *
     * @return The created stream.
     */
    fun stream(request: HTTPRequest, duplex: Boolean, onResult: suspend (StreamResult<Buffer>) -> Unit): Stream
}

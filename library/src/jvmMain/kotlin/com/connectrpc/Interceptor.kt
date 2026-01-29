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

package com.connectrpc

import com.connectrpc.http.HTTPRequest
import com.connectrpc.http.HTTPResponse
import com.connectrpc.http.UnaryHTTPRequest
import okio.Buffer

/**
 *  Interceptors can be registered with clients as a way to observe and/or alter outbound requests
 *  and inbound responses.
 *
 *  Interceptors are expected to be instantiated once per request/stream.
 */
interface Interceptor {
    // TODO: This interface and the StreamResult class should be internal.
    //       User-provided interceptors should have a better API that provides
    //       similar higher-level abstraction as the stream interfaces.

    /**
     * Invoked when a unary call is started. Provides a set of closures that will be called
     * as the request progresses, allowing the interceptor to alter request/response data.
     *
     * @return A new set of closures which can be used to read/alter request/response data.
     */
    fun unaryFunction(): UnaryFunction

    /**
     * Invoked when a streaming call is started. Provides a set of closures that will be called
     * as the stream progresses, allowing the interceptor to alter request/response data.
     *
     * NOTE: Closures may be called multiple times as the stream progresses (for example, as data
     * is sent/received over the stream). Furthermore, a guarantee is provided that each data chunk
     * will contain 1 full message (for Connect and gRPC, this includes the prefix and message
     * length bytes, followed by the actual message data).
     *
     * @return A new set of closures which can be used to read/alter request/response data.
     */
    fun streamFunction(): StreamFunction
}

class UnaryFunction(
    val requestFunction: (UnaryHTTPRequest) -> UnaryHTTPRequest = { it },
    val responseFunction: (HTTPResponse) -> HTTPResponse = { it },
)

class StreamFunction(
    val requestFunction: (HTTPRequest) -> HTTPRequest = { it },
    val requestBodyFunction: (Buffer) -> Buffer = { it },
    val streamResultFunction: (StreamResult<Buffer>) -> StreamResult<Buffer> = { it },
)

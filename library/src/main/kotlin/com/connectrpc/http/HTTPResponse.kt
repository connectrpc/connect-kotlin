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

package com.connectrpc.http

import com.connectrpc.Code
import com.connectrpc.ConnectError
import com.connectrpc.Headers
import com.connectrpc.Trailers
import okio.BufferedSource

/**
 * Unary HTTP response received from the server.
 */
class HTTPResponse(
    // The status code of the response.
    val code: Code,
    // Response headers specified by the server.
    val headers: Headers,
    // Body data provided by the server.
    val message: BufferedSource,
    // Trailers provided by the server.
    val trailers: Trailers,
    // Tracing information that can be used for logging or debugging network-level details.
    // This information is expected to change when switching protocols (i.e., from Connect to
    // gRPC-Web), as each protocol has different HTTP semantics.
    // null in cases where no response was received from the server.
    val tracingInfo: TracingInfo?,
    // The accompanying error, if the request failed.
    val error: ConnectError? = null
)

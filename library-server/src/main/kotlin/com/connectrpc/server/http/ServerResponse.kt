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

package com.connectrpc.server.http

import com.connectrpc.ConnectException
import com.connectrpc.Headers
import com.connectrpc.Trailers
import okio.Buffer

/**
 * Represents a server-side RPC response before protocol processing.
 *
 * Handlers produce this normalized representation, which is then
 * transformed by the protocol layer into wire format.
 */
sealed class ServerResponse {
    /**
     * Response headers to be sent to the client.
     */
    abstract val headers: Headers

    /**
     * Response trailers to be sent to the client.
     */
    abstract val trailers: Trailers

    /**
     * A successful response with a message body.
     */
    data class Success(
        override val headers: Headers = emptyMap(),
        override val trailers: Trailers = emptyMap(),
        /**
         * The response message body.
         */
        val message: Buffer,
    ) : ServerResponse()

    /**
     * An error response.
     */
    data class Failure(
        override val headers: Headers = emptyMap(),
        override val trailers: Trailers = emptyMap(),
        /**
         * The error that occurred.
         */
        val error: ConnectException,
    ) : ServerResponse()
}

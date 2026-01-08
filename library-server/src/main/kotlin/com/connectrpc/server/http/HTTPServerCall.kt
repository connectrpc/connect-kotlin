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

import com.connectrpc.Headers
import com.connectrpc.Trailers
import kotlinx.coroutines.flow.Flow
import okio.Buffer

/**
 * Abstraction for an HTTP server call.
 *
 * Each HTTP framework (Ktor, Netty, etc.) implements this interface
 * to integrate with the Connect server.
 */
interface HTTPServerCall {
    /**
     * The HTTP method (GET, POST, etc.).
     */
    val method: String

    /**
     * The request path (e.g., "/package.Service/Method").
     */
    val path: String

    /**
     * Request headers.
     */
    val requestHeaders: Headers

    /**
     * The Content-Type header value, or null if not present.
     */
    val contentType: String?

    /**
     * Reads the entire request body.
     * For unary RPCs.
     */
    suspend fun receiveBody(): Buffer

    /**
     * Sends response headers with the given HTTP status code.
     * Must be called before [respondBody] or [respondTrailers].
     */
    suspend fun respondHeaders(status: Int, headers: Headers)

    /**
     * Sends the response body.
     * For unary RPCs.
     */
    suspend fun respondBody(body: Buffer)

    /**
     * Sends trailers.
     * For HTTP/2, these are sent as actual trailers.
     * For HTTP/1.1 with Connect protocol, these may be encoded in the response body.
     */
    suspend fun respondTrailers(trailers: Trailers)

    /**
     * Receives a stream of messages from the client.
     * For client streaming and bidirectional streaming RPCs.
     * Each Buffer contains one envelope-encoded message.
     */
    suspend fun receiveStream(): Flow<Buffer>

    /**
     * Sends a stream of messages to the client.
     * For server streaming and bidirectional streaming RPCs.
     * Each Buffer should contain one envelope-encoded message.
     */
    suspend fun sendStream(messages: Flow<Buffer>)
}

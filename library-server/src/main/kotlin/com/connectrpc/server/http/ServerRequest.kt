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
import okio.Buffer
import kotlin.time.Duration

/**
 * Represents a server-side RPC request after protocol processing.
 *
 * This is the normalized representation used by handlers,
 * after protocol-specific details have been handled.
 */
data class ServerRequest(
    /**
     * The service name (e.g., "connectrpc.eliza.v1.ElizaService").
     */
    val serviceName: String,

    /**
     * The method name (e.g., "Say").
     */
    val methodName: String,

    /**
     * The full procedure path (e.g., "connectrpc.eliza.v1.ElizaService/Say").
     */
    val procedure: String,

    /**
     * Request headers after protocol processing.
     * Protocol-specific headers may be removed or transformed.
     */
    val headers: Headers,

    /**
     * The request message body (deserialized from the wire format).
     */
    val message: Buffer,

    /**
     * The timeout for this RPC, if specified by the client.
     */
    val timeout: Duration? = null,

    /**
     * The content type of the request.
     */
    val contentType: String,

    /**
     * Whether the request body is compressed.
     */
    val isCompressed: Boolean = false,

    /**
     * The compression algorithm used, if any.
     */
    val compression: String? = null,
)

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

package com.connectrpc.protocols

import com.connectrpc.Code
import com.connectrpc.ConnectErrorDetail
import com.connectrpc.ConnectException
import com.connectrpc.Headers
import com.connectrpc.SerializationStrategy

/**
 * Represents the parsed data structure from the GRPC trailers.
 */
internal data class GRPCCompletion(
    // The status code of the response. If null, the response indicated
    // "OK". Non-null indicates an error code.
    val code: Code?,
    // Message data.
    val message: String,
    // List of error details.
    val errorDetails: List<ConnectErrorDetail> = emptyList(),
    // Set to either message headers (or trailers) where the gRPC status was found.
    val metadata: Headers,
    // If true, this status was parsed from headers, in a "trailers-only" response.
    // Otherwise, the status was parsed from trailers.
    val trailersOnly: Boolean = false,
    // If false, this completion was synthesized and not actually present in metadata.
    val present: Boolean = true,
) {
    /**
     * Converts a completion into a [ConnectException] if the completion failed
     * @return a ConnectException on failure, null otherwise
     */
    fun toConnectExceptionOrNull(serializationStrategy: SerializationStrategy): ConnectException? {
        return if (code == null) {
            null
        } else {
            ConnectException(
                code = code,
                message = message,
                metadata = metadata,
            ).withErrorDetails(
                serializationStrategy.errorDetailParser(),
                errorDetails,
            )
        }
    }
}

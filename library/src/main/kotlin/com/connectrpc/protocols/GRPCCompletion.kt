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

package com.connectrpc.protocols

import com.connectrpc.Code
import com.connectrpc.ConnectError
import com.connectrpc.ConnectErrorDetail
import com.connectrpc.ErrorDetailParser
import com.connectrpc.Headers
import com.connectrpc.SerializationStrategy
import okio.ByteString

/**
 * Represents the parsed data structure from the GRPC trailers.
 */
internal data class GRPCCompletion(
    // The status code of the response.
    val code: Code,
    // The numerical status parsed from trailers.
    val status: Int?,
    // Message data.
    val message: ByteString,
    // List of error details.
    val errorDetails: List<ConnectErrorDetail>,
    // Set to either message headers (or trailers) where the gRPC status was found.
    val metadata: Headers
)

internal fun grpcCompletionToConnectError(completion: GRPCCompletion?, serializationStrategy: SerializationStrategy, error: Throwable?): ConnectError? {
    val code = completion?.code ?: Code.UNKNOWN
    if (error is ConnectError) {
        return error
    }
    if (error != null || code != Code.OK) {
        return ConnectError(
            code = code,
            errorDetailParser = serializationStrategy.errorDetailParser(),
            message = completion?.message?.utf8(),
            exception = error,
            details = completion?.errorDetails ?: emptyList(),
            metadata = completion?.metadata ?: emptyMap()
        )
    }
    // Successful call.
    return null
}

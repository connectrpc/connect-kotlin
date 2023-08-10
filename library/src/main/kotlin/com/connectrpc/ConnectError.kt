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

package com.connectrpc

import kotlin.reflect.KClass

/**
 * Typed error provided by Connect RPCs that may optionally wrap additional typed custom errors
 * using [details].
 */
data class ConnectError constructor(
    // The resulting status code.
    val code: Code,
    private val errorDetailParser: ErrorDetailParser? = null,
    // User-readable error message.
    override val message: String? = null,
    // Client-side exception that occurred, resulting in the error.
    val exception: Throwable? = null,
    // List of typed errors that were provided by the server.
    val details: List<ConnectErrorDetail> = emptyList(),
    // Additional key-values that were provided by the server.
    val metadata: Headers = emptyMap()
) : Throwable(message, exception) {

    /**
     * Unpacks values from [details] and returns the first matching error, if any.
     *
     * @return The unpacked typed error details, if available.
     */
    fun <E : Any> unpackedDetails(clazz: KClass<E>): List<E> {
        val parsedDetails = mutableListOf<E>()
        for (detail in details) {
            val unpackedMessage = errorDetailParser?.unpack(detail.pb, clazz)
            if (unpackedMessage != null) {
                parsedDetails.add(unpackedMessage)
            }
        }
        return parsedDetails
    }

    /**
     * Creates a new [ConnectError] with the specified [ErrorDetailParser].
     */
    fun setErrorParser(errorParser: ErrorDetailParser): ConnectError {
        return ConnectError(
            code,
            errorParser,
            message,
            exception,
            details,
            metadata
        )
    }
}

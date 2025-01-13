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

import kotlin.reflect.KClass

/**
 * Typed error provided by Connect RPCs that may optionally wrap additional typed custom errors
 * using [details].
 */
class ConnectException private constructor(
    // The resulting status code.
    val code: Code,
    // User-readable error message.
    override val message: String?,
    // Client-side exception that occurred, resulting in the error.
    val exception: Throwable?,
    // Additional key-values that were provided by the server.
    val metadata: Headers,
    // Optional parser for messages in details. Will be non-null if
    // details is non-empty.
    private val errorDetailParser: ErrorDetailParser?,
    // List of typed errors that were provided by the server.
    val details: List<ConnectErrorDetail>,
) : Exception(message, exception) {
    /**
     * Constructs a new ConnectException.
     */
    constructor (
        code: Code,
        message: String? = null,
        exception: Throwable? = null,
        metadata: Headers = emptyMap(),
    ) : this(code, message, exception, metadata, null, emptyList())

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
     * Creates a new [ConnectException] with the specified error details and
     * accompanying (non-null) detail parser.
     */
    fun withErrorDetails(errorParser: ErrorDetailParser, details: List<ConnectErrorDetail>): ConnectException {
        return ConnectException(
            code,
            message,
            exception,
            metadata,
            errorParser,
            details,
        )
    }

    /**
     * Creates a new [ConnectException] with the specified metadata.
     */
    fun withMetadata(metadata: Headers): ConnectException {
        return ConnectException(
            code,
            message,
            exception,
            metadata,
            errorDetailParser,
            details,
        )
    }
}

/**
 * Returns a ConnectException for the given cause. If ex is a ConnectException
 * then it is returned. Otherwise, it is wrapped in a ConnectException with
 * the given code.
 */
fun asConnectException(ex: Throwable, code: Code = Code.UNKNOWN): ConnectException {
    return if (ex is ConnectException) {
        ex
    } else {
        ConnectException(code = code, exception = ex)
    }
}

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

/**
 * Typed unary response from an RPC.
 */
sealed class ResponseMessage<Output>(
    // Response headers specified by the server.
    open val headers: Headers,
    // Trailers provided by the server.
    open val trailers: Trailers,
) {
    class Success<Output>(
        // The message.
        val message: Output,
        // Response headers specified by the server.
        headers: Headers,
        // Trailers provided by the server.
        trailers: Trailers,
    ) : ResponseMessage<Output>(headers, trailers) {
        override fun toString(): String {
            return "Success{message=$message,headers=$headers,trailers=$trailers}"
        }
    }

    class Failure<Output>(
        // The problem.
        val cause: ConnectException,
        // Response headers specified by the server.
        headers: Headers,
        // Trailers provided by the server.
        trailers: Trailers,
    ) : ResponseMessage<Output>(headers, trailers) {
        override fun toString(): String {
            return "Failure{cause=$cause,headers=$headers,trailers=$trailers}"
        }
    }

    fun <E> failure(function: (Failure<Output>) -> E?): E? {
        if (this is Failure) {
            return function(this)
        }
        return null
    }

    fun <E> success(function: (Success<Output>) -> E?): E? {
        if (this is Success) {
            return function(this)
        }
        return null
    }
}

/**
 * Returns the encapsulated [Throwable] exception if this instance represents [failure][ResponseMessage.Failure] or `null`
 * if it is [success][ResponseMessage.Success].
 */
fun ResponseMessage<*>.exceptionOrNull(): Throwable? {
    return when (this) {
        is ResponseMessage.Success -> null
        is ResponseMessage.Failure -> this.cause
    }
}

/**
 * Returns the result of [onSuccess] for the encapsulated value if this instance represents [success][ResponseMessage.Success]
 * or the result of [onFailure] function for the encapsulated [Throwable] exception if it is [failure][ResponseMessage.Failure].
 *
 * Note, that this function rethrows any [Throwable] exception thrown by [onSuccess] or by [onFailure] function.
 */
inline fun <R, T> ResponseMessage<T>.fold(
    onSuccess: (value: T) -> R,
    onFailure: (exception: ConnectException) -> R,
): R {
    return when (this) {
        is ResponseMessage.Success -> onSuccess(this.message)
        is ResponseMessage.Failure -> onFailure(this.cause)
    }
}

/**
 * Returns the encapsulated value if this instance represents [success][ResponseMessage.Success] or the
 * [defaultValue] if it is [failure][ResponseMessage.Failure].
 */
fun <T> ResponseMessage<T>.getOrDefault(defaultValue: T): T {
    return when (this) {
        is ResponseMessage.Success -> this.message
        is ResponseMessage.Failure -> defaultValue
    }
}

/**
 * Returns the encapsulated value if this instance represents [success][ResponseMessage.Success] or the
 * result of [onFailure] function for the encapsulated [Throwable] exception if it is [failure][ResponseMessage.Failure].
 *
 * Note, that this function rethrows any [Throwable] exception thrown by [onFailure] function.
 */
inline fun <R, T : R> ResponseMessage<T>.getOrElse(onFailure: (exception: ConnectException) -> R): R {
    return when (this) {
        is ResponseMessage.Success -> this.message
        is ResponseMessage.Failure -> onFailure(this.cause)
    }
}

/**
 * Returns the encapsulated value if this instance represents [success][ResponseMessage.Success] or `null`
 * if it is [failure][ResponseMessage.Failure].
 */
fun <T> ResponseMessage<T>.getOrNull(): T? {
    return when (this) {
        is ResponseMessage.Success -> this.message
        is ResponseMessage.Failure -> null
    }
}

/**
 * Returns the encapsulated value if this instance represents [success][ResponseMessage.Success] or throws the encapsulated [Throwable] exception
 * if it is [failure][ResponseMessage.Failure].
 */
fun <T> ResponseMessage<T>.getOrThrow(): T {
    return when (this) {
        is ResponseMessage.Success -> this.message
        is ResponseMessage.Failure -> throw this.cause
    }
}

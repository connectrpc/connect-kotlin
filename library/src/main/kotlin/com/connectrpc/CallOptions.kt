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

package com.connectrpc

/**
 * Represents options to use when invoking an RPC.
 */
sealed class CallOptions {
    /**
     * Request headers to send to the server.
     */
    abstract val headers: Headers

    // TODO: add call option for timeout

    companion object {
        /** Empty value that indicate the use of default options. */
        val empty = Builder().build()

        /** Shorthand for creating options with a single value for the request headers. */
        fun headers(headers: Headers): CallOptions {
            return Builder().headers(headers).build()
        }
    }

    /**
     * Builds a set of call options.
     */
    class Builder {
        private var headers: Headers = emptyMap()

        /** Sets the request headers to send. */
        fun headers(headers: Headers): Builder {
            this.headers = headers
            return this
        }

        /** Builds the options using the values accumulated so far. */
        fun build(): CallOptions {
            return CallOptionsImpl(headers)
        }
    }

    private data class CallOptionsImpl(
        override val headers: Headers,
    ) : CallOptions()
}

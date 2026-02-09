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
 * Enumeration of result states that can be received over streams.
 *
 * A typical stream receives [Headers] > [Message] > [Message] > [Message] ... > [Complete]
 */
sealed class StreamResult<Output> {
    // Headers have been received over the stream.
    class Headers<Output>(val headers: com.connectrpc.Headers) : StreamResult<Output>() {
        // TODO: This should include an HTTP status code, too. Computing an RPC code
        //       from the HTTP status code should be part of the protocol impl, not
        //       pushed down to the HTTPClientInterface impl.
        override fun toString(): String {
            return "Headers{headers=$headers}"
        }
    }

    // A response message has been received over the stream.
    class Message<Output>(val message: Output) : StreamResult<Output>() {
        override fun toString(): String {
            return "Message{message=$message}"
        }
    }

    // Stream is complete. Provides the end status code and optionally an error and trailers.
    class Complete<Output>(val cause: ConnectException? = null, val trailers: Trailers = emptyMap()) : StreamResult<Output>() {
        override fun toString(): String {
            return "Complete{cause=$cause,trailers=$trailers}"
        }
    }

    /**
     * Fold the different results into a single type.
     *
     * @param onHeaders Transform a Header result.
     * @param onMessage Transform a Message result.
     * @param onCompletion Transform a Completion result.
     */
    inline fun <Result> fold(
        onHeaders: (Headers<Output>) -> Result,
        onMessage: (Message<Output>) -> Result,
        onCompletion: (Complete<Output>) -> Result,
    ): Result {
        return when (this) {
            is Headers -> {
                onHeaders(this)
            }

            is Message -> {
                onMessage(this)
            }

            is Complete -> {
                onCompletion(this)
            }
        }
    }
}

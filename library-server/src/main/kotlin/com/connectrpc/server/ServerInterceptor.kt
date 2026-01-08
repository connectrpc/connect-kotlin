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

package com.connectrpc.server

import com.connectrpc.server.http.ServerRequest
import com.connectrpc.server.http.ServerResponse

/**
 * Server interceptor for processing requests and responses.
 *
 * Interceptors can be used for logging, authentication, metrics, etc.
 * They are invoked in order for requests and in reverse order for responses.
 */
interface ServerInterceptor {
    /**
     * Returns the unary function for processing unary RPCs.
     */
    fun unaryFunction(): ServerUnaryFunction

    /**
     * Returns the stream function for processing streaming RPCs.
     */
    fun streamFunction(): ServerStreamFunction
}

/**
 * Functions for processing unary RPCs on the server side.
 *
 * The request function is called after receiving the request,
 * and the response function is called before sending the response.
 */
class ServerUnaryFunction(
    /**
     * Transforms the request after receiving it from the client.
     * Called before the handler is invoked.
     */
    val requestFunction: (ServerRequest) -> ServerRequest = { it },

    /**
     * Transforms the response before sending it to the client.
     * Called after the handler returns.
     */
    val responseFunction: (ServerResponse) -> ServerResponse = { it },
)

/**
 * Functions for processing streaming RPCs on the server side.
 */
class ServerStreamFunction(
    /**
     * Transforms the request after receiving it from the client.
     */
    val requestFunction: (ServerRequest) -> ServerRequest = { it },

    /**
     * Transforms each incoming message from the client stream.
     */
    val requestMessageFunction: (ByteArray) -> ByteArray = { it },

    /**
     * Transforms each outgoing message to the client stream.
     */
    val responseMessageFunction: (ByteArray) -> ByteArray = { it },
)

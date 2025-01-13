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
 * Completion parser is a helper for parsing stream completions.
 *
 * Both the Connect and gRPC protocol relies on the
 * status.proto as the structure of error payloads.
 */
interface ErrorDetailParser {
    /**
     * Unpack the given Any payload into the input class type.
     */
    fun <E : Any> unpack(any: AnyError, clazz: KClass<E>): E?

    /**
     * Parse the given bytes for a list of error details. The given
     * bytes will be the serialized form of a google.rpc.Status
     * Protobuf message.
     */
    fun parseDetails(bytes: ByteArray): List<ConnectErrorDetail>
}

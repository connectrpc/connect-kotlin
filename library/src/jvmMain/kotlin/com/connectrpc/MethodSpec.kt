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
 * Represents the minimum set of information to execute an RPC method.
 * Primarily used in generated code.
 *
 * @param path The path of the request.
 * @param requestClass The Kotlin Class for the request message.
 * @param responseClass The Kotlin Class for the response message.
 * @param idempotency The declared idempotency of a method.
 * @param streamType The method's stream type.
 */
class MethodSpec<Input : Any, Output : Any>(
    val path: String,
    val requestClass: KClass<Input>,
    val responseClass: KClass<Output>,
    val streamType: StreamType,
    val idempotency: Idempotency = Idempotency.UNKNOWN,
)

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

package build.buf.connect

import kotlin.reflect.KClass

internal object Method {
    internal const val GET_METHOD = "GET"
    internal const val POST_METHOD = "POST"
}

/**
 * Represents the minimum set of information to execute an RPC method.
 * Primarily used in generated code.
 *
 * @param path The path of the request.
 * @param requestClass The Kotlin Class for the request message.
 * @param responseClass The Kotlin Class for the response message.
 * @param idempotency The declared idempotency of a method.
 * @param method The http method of a request.
 */
class MethodSpec<Input : Any, Output : Any>(
    val path: String,
    val requestClass: KClass<Input>,
    val responseClass: KClass<Output>,
    val idempotency: Idempotency = Idempotency.IDEMPOTENCY_UNKNOWN,
    val method: String = Method.POST_METHOD
)

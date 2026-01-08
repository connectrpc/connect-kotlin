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

package com.connectrpc.protocgen.connect.internal

internal const val CALLBACK_SIGNATURE = "generateCallbackMethods"
internal const val COROUTINE_SIGNATURE = "generateCoroutineMethods"
internal const val BLOCKING_UNARY_SIGNATURE = "generateBlockingUnaryMethods"
internal const val SERVER_SIGNATURE = "generateServerMethods"

/**
 * The protoc plugin configuration class representation.
 */
internal data class Configuration(
    // Enable or disable callback signature generation.
    val generateCallbackMethods: Boolean,
    // Enable or disable coroutine signature generation.
    val generateCoroutineMethods: Boolean,
    // Enable or disable blocking unary signature generation.
    val generateBlockingUnaryMethods: Boolean,
    // Enable or disable server handler generation.
    val generateServerMethods: Boolean,
)

/**
 * Parse options passed as a string.
 *
 * Key values are parsed with `parseGeneratorParameter()`.
 * The key values are expected to be in camel casing but
 * will internally translate from snake casing to camel
 * casing.
 */
internal fun parse(input: String): Configuration {
    val parameters = parseGeneratorParameter(input)
    return Configuration(
        generateCallbackMethods = parameters[CALLBACK_SIGNATURE]?.toBoolean() ?: false,
        // Defaulted to true.
        generateCoroutineMethods = parameters[COROUTINE_SIGNATURE]?.toBoolean() ?: true,
        generateBlockingUnaryMethods = parameters[BLOCKING_UNARY_SIGNATURE]?.toBoolean() ?: false,
        generateServerMethods = parameters[SERVER_SIGNATURE]?.toBoolean() ?: false,
    )
}

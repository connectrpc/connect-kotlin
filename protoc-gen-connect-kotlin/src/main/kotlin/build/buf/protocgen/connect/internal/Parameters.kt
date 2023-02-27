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

package build.buf.protocgen.connect.internal

internal const val CALLBACK_SIGNATURE = "callbackSignature"

/**
 * The protoc plugin configuration class representation.
 */
internal data class Configuration(
    // Enable or disable callback signature generation.
    val callbackSignature: Boolean
)

/**
 * Parse options passed as a string.
 *
 * Key values are parsed with `parseGeneratorParameter()`.
 * The key values are expected to be in camel casing but
 * will internally translate from snake casing to camel
 * casing.
 */
internal fun parse(parameter: String): Configuration {
    val parameters = parseGeneratorParameter(parameter)
    return Configuration(
        callbackSignature = parameters[CALLBACK_SIGNATURE]?.toBoolean() ?: false
    )
}

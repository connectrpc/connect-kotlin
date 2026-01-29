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

package com.connectrpc.protocols

import com.connectrpc.Headers
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal class ErrorPayloadJSON(
    @Json(name = "code") val code: String?,
    @Json(name = "message") val message: String?,
    @Json(name = "details") val details: List<ErrorDetailPayloadJSON>?,
)

@JsonClass(generateAdapter = true)
internal class ErrorDetailPayloadJSON(
    @Json(name = "type") val type: String?,
    @Json(name = "value") val value: String?,
)

@JsonClass(generateAdapter = true)
internal class EndStreamResponseJSON(
    @Json(name = "error") val error: ErrorPayloadJSON?,
    @Json(name = "metadata") val metadata: Headers?,
)

internal fun contentTypeIsJSON(contentType: String): Boolean {
    // TODO: This could be more robust, like actually parsing the content-type.
    // There exists a good helper for that, but it's in okhttp, which we intentionally
    // don't have as a dep for this module, which aims to be agnostic of the actual
    // HTTP client implementation to use.
    return contentType == "application/json" || contentType == "application/json; charset=utf-8"
}

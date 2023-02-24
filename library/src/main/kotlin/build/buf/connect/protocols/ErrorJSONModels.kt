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

package build.buf.connect.protocols

import com.squareup.moshi.Json

internal class ErrorPayloadJSON(
    @Json(name = "code") val code: String?,
    @Json(name = "message") val message: String?,
    @Json(name = "details") val details: List<ErrorDetailPayloadJSON>?,
)

internal class ErrorDetailPayloadJSON(
    @Json(name = "type") val type: String?,
    @Json(name = "value") val value: String?,
)

internal class EndStreamResponseJSON(
    @Json(name = "error") val error: ErrorPayloadJSON?,
    @Json(name = "metadata") val metadata: Map<String, List<String>>?,
)

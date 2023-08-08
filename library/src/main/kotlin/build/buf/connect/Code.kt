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

// The zero code in gRPC is OK, which indicates that the operation was a
// success. We don't define a constant for it because it overlaps awkwardly
// with Go's error semantics: what does it mean to have a non-nil error with
// an OK status? (Also, the Connect protocol doesn't use a code for
// successes.)
enum class Code(val codeName: String, val value: Int) {
    OK("ok", 0),
    CANCELED("canceled", 1),
    UNKNOWN("unknown", 2),
    INVALID_ARGUMENT("invalid_argument", 3),
    DEADLINE_EXCEEDED("deadline_exceeded", 4),
    NOT_FOUND("not_found", 5),
    ALREADY_EXISTS("already_exists", 6),
    PERMISSION_DENIED("permission_denied", 7),
    RESOURCE_EXHAUSTED("resource_exhausted", 8),
    FAILED_PRECONDITION("failed_precondition", 9),
    ABORTED("aborted", 10),
    OUT_OF_RANGE("out_of_range", 11),
    UNIMPLEMENTED("unimplemented", 12),
    INTERNAL_ERROR("internal", 13),
    UNAVAILABLE("unavailable", 14),
    DATA_LOSS("data_loss", 15),
    UNAUTHENTICATED("unauthenticated", 16);

    companion object {
        // https://connect.build/docs/protocol#http-to-error-code
        fun fromHTTPStatus(status: Int): Code {
            return when (status) {
                200 -> OK
                400 -> INVALID_ARGUMENT
                401 -> UNAUTHENTICATED
                403 -> PERMISSION_DENIED
                404 -> UNIMPLEMENTED
                408 -> DEADLINE_EXCEEDED
                409 -> ABORTED
                412 -> FAILED_PRECONDITION
                413 -> RESOURCE_EXHAUSTED
                415 -> INTERNAL_ERROR
                429 -> UNAVAILABLE
                431 -> RESOURCE_EXHAUSTED
                502, 503, 504 -> UNAVAILABLE
                else -> UNKNOWN
            }
        }
        fun fromName(name: String?): Code {
            for (value in values()) {
                if (value.codeName == name) {
                    return value
                }
            }
            return UNKNOWN
        }
        fun fromValue(value: Int?): Code {
            if (value == null) {
                return UNKNOWN
            }
            return values().first { code -> code.value == value }
        }
    }
}

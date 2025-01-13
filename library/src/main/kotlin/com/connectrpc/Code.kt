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
 * Enumeration of RPC error codes.
 *
 * In gRPC, there is a zero value for "OK",
 * but we don't define one because these are only used in the face of errors.
 * Successful operations don't need an "OK" code since they are otherwise
 * identified by their not having an exception. So only exceptions need a
 * code (in which case "OK" is not valid).
 */
enum class Code(val codeName: String, val value: Int) {
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
    INTERNAL_ERROR("internal", 13), // TODO: rename enum value to INTERNAL
    UNAVAILABLE("unavailable", 14),
    DATA_LOSS("data_loss", 15),
    UNAUTHENTICATED("unauthenticated", 16),
    ;

    companion object {
        // https://connectrpc.com/docs/protocol#http-to-error-code
        fun fromHTTPStatus(status: Int?): Code {
            return when (status) {
                null -> UNKNOWN
                400 -> INTERNAL_ERROR
                401 -> UNAUTHENTICATED
                403 -> PERMISSION_DENIED
                404 -> UNIMPLEMENTED
                429, 502, 503, 504 -> UNAVAILABLE
                else -> UNKNOWN
            }
        }
        fun fromName(name: String?, ifNotKnown: Code = UNKNOWN): Code {
            if (name == null) {
                return ifNotKnown
            }
            for (value in values()) {
                if (value.codeName == name) {
                    return value
                }
            }
            return ifNotKnown
        }
        fun fromValue(value: Int?): Code? {
            if (value == null) {
                return UNKNOWN
            }
            if (value == 0) {
                return null // 0 means OK, so no error code
            }
            val code = values().firstOrNull { code -> code.value == value }
            return code ?: UNKNOWN
        }
    }
}

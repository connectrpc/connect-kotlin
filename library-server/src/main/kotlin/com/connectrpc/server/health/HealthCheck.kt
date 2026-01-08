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

package com.connectrpc.server.health

/**
 * Health check serving status.
 *
 * Based on grpc.health.v1.HealthCheckResponse.ServingStatus.
 */
enum class ServingStatus(val value: Int) {
    /** Status unknown. */
    UNKNOWN(0),

    /** The service is healthy and serving requests. */
    SERVING(1),

    /** The service is not serving requests. */
    NOT_SERVING(2),

    /** The requested service is not known to the server. */
    SERVICE_UNKNOWN(3),
    ;

    companion object {
        fun fromValue(value: Int): ServingStatus {
            return entries.find { it.value == value } ?: UNKNOWN
        }
    }
}

/**
 * Health check request.
 *
 * Based on grpc.health.v1.HealthCheckRequest.
 *
 * @param service The service name to check. Empty string means overall server health.
 */
data class HealthCheckRequest(
    val service: String = "",
)

/**
 * Health check response.
 *
 * Based on grpc.health.v1.HealthCheckResponse.
 *
 * @param status The serving status of the requested service.
 */
data class HealthCheckResponse(
    val status: ServingStatus,
)

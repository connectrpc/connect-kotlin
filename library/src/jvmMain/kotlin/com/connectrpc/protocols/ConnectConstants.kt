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

const val ACCEPT_ENCODING = "accept-encoding"
const val CONTENT_ENCODING = "content-encoding"
const val CONTENT_TYPE = "content-type"
const val CONNECT_STREAMING_CONTENT_ENCODING = "connect-content-encoding"
const val CONNECT_STREAMING_ACCEPT_ENCODING = "connect-accept-encoding"
const val CONNECT_PROTOCOL_VERSION_KEY = "connect-protocol-version"
const val CONNECT_PROTOCOL_VERSION_VALUE = "1"
const val CONNECT_TIMEOUT_MS = "connect-timeout-ms"

const val GRPC_ACCEPT_ENCODING = "grpc-accept-encoding"
const val GRPC_ENCODING = "grpc-encoding"
const val GRPC_MESSAGE_TRAILER = "grpc-message"
const val GRPC_STATUS_DETAILS_TRAILERS = "grpc-status-details-bin"
const val GRPC_STATUS_TRAILER = "grpc-status"
const val GRPC_TE_HEADER = "te"
const val GRPC_WEB_USER_AGENT = "x-user-agent"
const val GRPC_TIMEOUT = "grpc-timeout"

const val USER_AGENT = "user-agent"

object ConnectConstants {
    /** Version number of the connect-kotlin library */
    val VERSION = javaClass.`package`?.implementationVersion ?: "dev"
}

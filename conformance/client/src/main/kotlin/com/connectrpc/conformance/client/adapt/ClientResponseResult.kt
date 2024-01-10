// Copyright 2022-2023 The Connect Authors
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

package com.connectrpc.conformance.client.adapt

import com.connectrpc.ConnectException
import com.connectrpc.Headers
import com.google.protobuf.MessageLite

/**
 * Represents the result of issuing an RPC.
 *
 * This corresponds to the connectrpc.conformance.v1.ClientResponseResult
 * proto message. Its presence is to provide a representation that
 * doesn't rely on either the standard or lite Protobuf runtime.
 */
class ClientResponseResult(
    val headers: Headers = emptyMap(),
    val payloads: List<MessageLite> = emptyList(),
    val trailers: Headers = emptyMap(),
    val error: ConnectException? = null,
    val numUnsentRequests: Int = 0,
)

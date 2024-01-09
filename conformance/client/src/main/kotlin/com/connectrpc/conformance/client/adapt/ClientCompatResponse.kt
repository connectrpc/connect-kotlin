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

/**
 * Represents the response of a conformance request. This
 * describes the RPC result of invoking an RPC for a particular
 * conformance test case.
 *
 * This corresponds to the connectrpc.conformance.v1.ClientCompatResponse
 * proto message. Its presence is to provide a representation that
 * doesn't rely on either the standard or lite Protobuf runtime.
 *
 * This can represent a result received from an RPC server or an
 * error that prevented the RPC from being invoked.
 */
data class ClientCompatResponse(
    val testName: String,
    val result: Result,
) {

    sealed class Result {
        class ResponseResult(val response: ClientResponseResult) : Result()
        class ErrorResult(val error: String) : Result()
    }
}

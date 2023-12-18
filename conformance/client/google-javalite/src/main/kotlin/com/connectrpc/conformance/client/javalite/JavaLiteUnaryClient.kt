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

package com.connectrpc.conformance.client.javalite

import com.connectrpc.Headers
import com.connectrpc.ResponseMessage
import com.connectrpc.UnaryBlockingCall
import com.connectrpc.conformance.client.adapt.UnaryClient
import com.connectrpc.http.Cancelable
import com.connectrpc.lite.connectrpc.conformance.v1.ConformanceServiceClient
import com.connectrpc.lite.connectrpc.conformance.v1.UnaryRequest
import com.connectrpc.lite.connectrpc.conformance.v1.UnaryResponse

class JavaLiteUnaryClient(
    private val client: ConformanceServiceClient,
) : UnaryClient<UnaryRequest, UnaryResponse>(
    UnaryRequest.getDefaultInstance(),
    UnaryResponse.getDefaultInstance(),
) {
    override suspend fun execute(
        req: UnaryRequest,
        headers: Headers,
    ): ResponseMessage<UnaryResponse> {
        return client.unary(req, headers)
    }

    override fun execute(
        req: UnaryRequest,
        headers: Headers,
        onFinish: (ResponseMessage<UnaryResponse>) -> Unit,
    ): Cancelable {
        return client.unary(req, headers, onFinish)
    }

    override fun blocking(req: UnaryRequest, headers: Headers): UnaryBlockingCall<UnaryResponse> {
        return client.unaryBlocking(req, headers)
    }
}

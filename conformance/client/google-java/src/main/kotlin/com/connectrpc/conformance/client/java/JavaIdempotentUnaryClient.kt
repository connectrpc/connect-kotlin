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

package com.connectrpc.conformance.client.java

import com.connectrpc.CallOptions
import com.connectrpc.ResponseMessage
import com.connectrpc.UnaryBlockingCall
import com.connectrpc.conformance.client.adapt.UnaryClient
import com.connectrpc.conformance.v1.ConformanceServiceClient
import com.connectrpc.conformance.v1.IdempotentUnaryRequest
import com.connectrpc.conformance.v1.IdempotentUnaryResponse
import com.connectrpc.http.Cancelable

class JavaIdempotentUnaryClient(
    private val client: ConformanceServiceClient,
) : UnaryClient<IdempotentUnaryRequest, IdempotentUnaryResponse>(
    IdempotentUnaryRequest.getDefaultInstance(),
    IdempotentUnaryResponse.getDefaultInstance(),
) {
    override suspend fun execute(req: IdempotentUnaryRequest, options: CallOptions): ResponseMessage<IdempotentUnaryResponse> {
        return client.idempotentUnary(req, options)
    }

    override fun execute(
        req: IdempotentUnaryRequest,
        options: CallOptions,
        onFinish: (ResponseMessage<IdempotentUnaryResponse>) -> Unit,
    ): Cancelable {
        return client.idempotentUnary(req, options, onFinish)
    }

    override fun blocking(req: IdempotentUnaryRequest, options: CallOptions): UnaryBlockingCall<IdempotentUnaryResponse> {
        return client.idempotentUnaryBlocking(req, options)
    }
}

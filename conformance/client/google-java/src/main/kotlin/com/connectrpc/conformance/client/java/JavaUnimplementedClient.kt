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
import com.connectrpc.conformance.v1.UnimplementedRequest
import com.connectrpc.conformance.v1.UnimplementedResponse
import com.connectrpc.http.Cancelable

class JavaUnimplementedClient(
    private val client: ConformanceServiceClient,
) : UnaryClient<UnimplementedRequest, UnimplementedResponse>(
    UnimplementedRequest.getDefaultInstance(),
    UnimplementedResponse.getDefaultInstance(),
) {
    override suspend fun execute(req: UnimplementedRequest, options: CallOptions): ResponseMessage<UnimplementedResponse> {
        return client.unimplemented(req, options)
    }

    override fun execute(
        req: UnimplementedRequest,
        options: CallOptions,
        onFinish: (ResponseMessage<UnimplementedResponse>) -> Unit,
    ): Cancelable {
        return client.unimplemented(req, options, onFinish)
    }

    override fun blocking(req: UnimplementedRequest, options: CallOptions): UnaryBlockingCall<UnimplementedResponse> {
        return client.unimplementedBlocking(req, options)
    }
}

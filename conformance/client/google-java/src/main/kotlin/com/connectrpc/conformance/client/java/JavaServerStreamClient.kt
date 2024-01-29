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

import com.connectrpc.Headers
import com.connectrpc.conformance.client.adapt.ResponseStream
import com.connectrpc.conformance.client.adapt.ServerStreamClient
import com.connectrpc.conformance.v1.ConformanceServiceClient
import com.connectrpc.conformance.v1.ServerStreamRequest
import com.connectrpc.conformance.v1.ServerStreamResponse

class JavaServerStreamClient(
    private val client: ConformanceServiceClient,
) : ServerStreamClient<ServerStreamRequest, ServerStreamResponse>(
    ServerStreamRequest.getDefaultInstance(),
    ServerStreamResponse.getDefaultInstance(),
) {
    override suspend fun execute(req: ServerStreamRequest, headers: Headers): ResponseStream<ServerStreamResponse> {
        val stream = client.serverStream(headers)
        val sendResult: Result<Unit>
        try {
            sendResult = stream.sendAndClose(req)
            if (sendResult.isFailure) {
                throw sendResult.exceptionOrNull()!!
            }
        } catch (ex: Throwable) {
            stream.receiveClose()
            throw ex
        }
        stream.sendAndClose(req)
        return ResponseStream.new(stream)
    }
}

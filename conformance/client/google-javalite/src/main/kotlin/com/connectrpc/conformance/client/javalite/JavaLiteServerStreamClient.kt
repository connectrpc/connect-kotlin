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

package com.connectrpc.conformance.client.javalite

import com.connectrpc.Headers
import com.connectrpc.conformance.client.adapt.ResponseStream
import com.connectrpc.conformance.client.adapt.ServerStreamClient
import com.connectrpc.conformance.v1.ConformanceServiceClient
import com.connectrpc.conformance.v1.ServerStreamRequest
import com.connectrpc.conformance.v1.ServerStreamResponse
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

class JavaLiteServerStreamClient(
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
                // It can't be because stream.sendClose was already closed. So the operation
                // must have already failed. Extract the reason via a call to receive. But
                // if something is awry, don't block forever on the receive call.
                try {
                    withTimeout(50) {
                        // Waits up to 50 milliseconds.
                        stream.responseChannel().receive()
                    }
                } catch (_: TimeoutCancellationException) {
                    // Receive did not complete :(
                } catch (ex: Throwable) {
                    throw ex
                }
                // Either receive did not complete or it did not fail (which
                // shouldn't actually be possible).
                throw sendResult.exceptionOrNull()!!
            }
        } catch (ex: Throwable) {
            stream.receiveClose()
            throw ex
        }
        return ResponseStream.new(stream)
    }
}

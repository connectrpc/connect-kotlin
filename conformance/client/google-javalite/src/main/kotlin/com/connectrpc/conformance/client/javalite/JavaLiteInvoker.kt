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

import com.connectrpc.conformance.client.adapt.BidiStreamClient
import com.connectrpc.conformance.client.adapt.ClientStreamClient
import com.connectrpc.conformance.client.adapt.Invoker
import com.connectrpc.conformance.client.adapt.ServerStreamClient
import com.connectrpc.conformance.client.adapt.UnaryClient
import com.connectrpc.conformance.v1.ConformanceServiceClient
import com.connectrpc.impl.ProtocolClient

class JavaLiteInvoker(
    protocolClient: ProtocolClient,
) : Invoker {
    private val client = ConformanceServiceClient(protocolClient)

    override fun unaryClient(): UnaryClient<*, *> {
        return JavaLiteUnaryClient(client)
    }

    override fun idempotentUnaryClient(): UnaryClient<*, *> {
        return JavaLiteIdempotentUnaryClient(client)
    }

    override fun unimplementedClient(): UnaryClient<*, *> {
        return JavaLiteUnimplementedClient(client)
    }

    override fun clientStreamClient(): ClientStreamClient<*, *> {
        return JavaLiteClientStreamClient(client)
    }

    override fun serverStreamClient(): ServerStreamClient<*, *> {
        return JavaLiteServerStreamClient(client)
    }

    override fun bidiStreamClient(): BidiStreamClient<*, *> {
        return JavaLiteBidiStreamClient(client)
    }
}

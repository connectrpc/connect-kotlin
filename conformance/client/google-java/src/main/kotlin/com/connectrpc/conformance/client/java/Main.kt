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

import com.connectrpc.conformance.client.Client
import com.connectrpc.conformance.client.ClientArgs
import com.connectrpc.conformance.client.ConformanceClientLoop

fun main(args: Array<String>) {
    val clientArgs = ClientArgs.parseArgs(args)
    val loop = ConformanceClientLoop(
        JavaHelpers::unmarshalRequest,
        JavaHelpers::marshalResponse,
        clientArgs.verbosity,
    )
    val client = Client(
        args = clientArgs,
        invokerFactory = ::JavaInvoker,
        serializationFactory = JavaHelpers::serializationStrategy,
        payloadExtractor = JavaHelpers::extractPayload,
    )
    loop.run(System.`in`, System.out, client)
    // TODO: catch any exception for better error output/logging?
}

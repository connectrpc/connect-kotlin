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

package com.connectrpc.examples.kotlin

import com.connectrpc.Code
import com.connectrpc.ConnectException
import com.connectrpc.ProtocolClientConfig
import com.connectrpc.eliza.v1.ElizaServiceClient
import com.connectrpc.eliza.v1.converseRequest
import com.connectrpc.extensions.GoogleJavaProtobufStrategy
import com.connectrpc.impl.ProtocolClient
import com.connectrpc.okhttp.ConnectOkHttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.time.Duration

class Main {
    companion object {

        @JvmStatic
        fun main(args: Array<String>) {
            runBlocking {
                val host = "https://demo.connectrpc.com"
                val okHttpClient = OkHttpClient()
                    .newBuilder()
                    .readTimeout(Duration.ofMinutes(10))
                    .writeTimeout(Duration.ofMinutes(10))
                    .callTimeout(Duration.ofMinutes(10))
                    .build()
                val client = ProtocolClient(
                    httpClient = ConnectOkHttpClient(okHttpClient),
                    ProtocolClientConfig(
                        host = host,
                        serializationStrategy = GoogleJavaProtobufStrategy(),
                    ),
                )
                val elizaServiceClient = ElizaServiceClient(client)
                try {
                    connectStreaming(elizaServiceClient)
                } finally {
                    okHttpClient.dispatcher.executorService.shutdown()
                }
            }
        }

        private suspend fun connectStreaming(elizaServiceClient: ElizaServiceClient) {
            val stream = elizaServiceClient.converse()
            withContext(Dispatchers.IO) {
                // Add the message the user is sending to the views.
                stream.send(converseRequest { sentence = "hello" })
                stream.sendClose()
                for (streamResult in stream.resultChannel()) {
                    streamResult.maybeFold(
                        onMessage = { result ->
                            // Update the view with the response.
                            val elizaResponse = result.message
                            println(elizaResponse.sentence)
                        },
                        onCompletion = { result ->
                            if (result.code != Code.OK) {
                                val connectErr = result.connectException()
                                if (connectErr != null) {
                                    throw connectErr
                                }
                                throw ConnectException(code = result.code, metadata = result.trailers)
                            }
                        },
                    )
                }
            }
        }
    }
}

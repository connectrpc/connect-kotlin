// Copyright 2022-2023 Buf Technologies, Inc.
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

package build.buf.connect.examples.kotlin

import build.buf.connect.ProtocolClientConfig
import build.buf.connect.demo.eliza.v1.ConverseRequest
import build.buf.connect.demo.eliza.v1.ElizaServiceClient
import build.buf.connect.demo.eliza.v1.SayRequest
import build.buf.connect.extensions.GoogleJavaProtobufStrategy
import build.buf.connect.impl.ProtocolClient
import build.buf.connect.okhttp.ConnectOkHttpClient
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
                val host = "https://demo.connect.build"
                val client = ProtocolClient(
                    httpClient = ConnectOkHttpClient(
                        OkHttpClient()
                            .newBuilder()
                            .readTimeout(Duration.ofMinutes(10))
                            .writeTimeout(Duration.ofMinutes(10))
                            .callTimeout(Duration.ofMinutes(10))
                            .build()
                    ),
                    ProtocolClientConfig(
                        host = host,
                        serializationStrategy = GoogleJavaProtobufStrategy()
                    )
                )
                val elizaServiceClient = ElizaServiceClient(client)
                talkToElizaSuspended(elizaServiceClient)
                talkToEliza(elizaServiceClient)
            }
        }

        private suspend fun talkToElizaSuspended(elizaServiceClient: ElizaServiceClient) {
            withContext(Dispatchers.IO) {
                val response = elizaServiceClient.say(SayRequest.newBuilder().setSentence("hello").build())
                response.success { result ->
                    println(result.message)
                }
            }
        }

        private fun talkToEliza(elizaServiceClient: ElizaServiceClient) {
            val cancelable = elizaServiceClient.say(SayRequest.newBuilder().setSentence("hello").build()) { response ->
                response.success { result ->
                    println(result.message)
                }
            }
            // cancelable() can be used to cancel the underlying request.
            cancelable()
        }
    }
}

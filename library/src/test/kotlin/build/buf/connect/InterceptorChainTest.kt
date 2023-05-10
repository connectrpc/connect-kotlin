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

package build.buf.connect

import build.buf.connect.http.HTTPRequest
import build.buf.connect.http.HTTPResponse
import build.buf.connect.http.TracingInfo
import build.buf.connect.protocols.Envelope
import build.buf.connect.protocols.NetworkProtocol
import okio.Buffer
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import java.net.URL

private val methodSpec = MethodSpec(
    path = "",
    requestClass = Any::class,
    responseClass = Any::class,
)

class InterceptorChainTest {
    private lateinit var streamingChain: StreamFunction
    private lateinit var unaryChain: UnaryFunction

    @Before
    fun setup() {
        val interceptorFactories = listOf(
            { _: ProtocolClientConfig ->
                SimpleInterceptor("1")
            },
            { _: ProtocolClientConfig ->
                SimpleInterceptor("2")
            },
            { _: ProtocolClientConfig ->
                SimpleInterceptor("3")
            },
            { _: ProtocolClientConfig ->
                SimpleInterceptor("4")
            }
        )
        val protocolClientConfig = ProtocolClientConfig(
            host = "host",
            serializationStrategy = mock { },
            networkProtocol = NetworkProtocol.CONNECT,
            interceptors = interceptorFactories
        )
        unaryChain = protocolClientConfig.createInterceptorChain()
        streamingChain = protocolClientConfig.createStreamingInterceptorChain()
    }

    @Test
    fun fifo_request_unary() {
        val response = unaryChain.requestFunction(HTTPRequest(URL("https://buf.build"), "", emptyMap(), null, methodSpec))
        assertThat(response.headers.get("id")).containsExactly("1", "2", "3", "4")
    }

    @Test
    fun lifo_response_unary() {
        val response = unaryChain.responseFunction(HTTPResponse(Code.OK, emptyMap(), Buffer(), emptyMap(), null))
        assertThat(response.headers.get("id")).containsExactly("4", "3", "2", "1")
    }

    @Test
    fun fifo_request_stream() {
        val request = streamingChain.requestFunction(HTTPRequest(URL("https://buf.build"), "", emptyMap(), null, methodSpec))
        assertThat(request.headers.get("id")).containsExactly("1", "2", "3", "4")
    }

    @Test
    fun fifo_request_body() {
        val request = streamingChain.requestBodyFunction(Buffer())
        val (_, unpacked) = Envelope.unpackWithHeaderByte(request)
        assertThat(unpacked.readUtf8()).isEqualTo("1234")
    }

    @Test
    fun lifo_stream_result() {
        val streamResult = streamingChain.streamResultFunction(StreamResult.Headers(emptyMap())) as StreamResult.Headers
        assertThat(streamResult.headers.get("id")).containsExactly("4", "3", "2", "1")
    }

    @Test
    fun unary_tracing_info() {
        val response = unaryChain.responseFunction(HTTPResponse(Code.OK, emptyMap(), Buffer(), emptyMap(), TracingInfo(888)))
        assertThat(response.tracingInfo!!.httpStatus).isEqualTo(888)
    }

    private class SimpleInterceptor(val id: String) : Interceptor {
        override fun unaryFunction(): UnaryFunction {
            return UnaryFunction(
                requestFunction = {
                    val headers = it.headers.toMutableMap()
                    val sequence = headers.get("id")?.toMutableList() ?: mutableListOf()
                    sequence.add(id)
                    headers.put("id", sequence)
                    HTTPRequest(
                        it.url,
                        it.contentType,
                        headers,
                        it.message,
                        methodSpec
                    )
                },
                responseFunction = {
                    val headers = it.headers.toMutableMap()
                    val sequence = headers.get("id")?.toMutableList() ?: mutableListOf()
                    sequence.add(id)
                    headers.put("id", sequence)
                    HTTPResponse(
                        it.code,
                        headers,
                        it.message,
                        it.trailers,
                        it.tracingInfo,
                        it.error
                    )
                }
            )
        }

        override fun streamFunction(): StreamFunction {
            return StreamFunction(
                requestFunction = {
                    val headers = it.headers.toMutableMap()
                    val sequence = headers.get("id")?.toMutableList() ?: mutableListOf()
                    sequence.add(id)
                    headers.put("id", sequence)
                    HTTPRequest(
                        it.url,
                        it.contentType,
                        headers,
                        it.message,
                        methodSpec
                    )
                },
                requestBodyFunction = {
                    it.writeString(id, Charsets.UTF_8)
                },
                streamResultFunction = {
                    it.fold(
                        onHeaders = {
                            val headers = it.headers.toMutableMap()
                            val sequence = headers.get("id")?.toMutableList() ?: mutableListOf()
                            sequence.add(id)
                            headers.put("id", sequence)
                            StreamResult.Headers(headers)
                        },
                        onMessage = { it },
                        onCompletion = { it }
                    )
                }
            )
        }
    }
}

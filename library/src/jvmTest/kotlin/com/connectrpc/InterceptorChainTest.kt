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

package com.connectrpc

import com.connectrpc.http.HTTPRequest
import com.connectrpc.http.HTTPResponse
import com.connectrpc.http.UnaryHTTPRequest
import com.connectrpc.http.clone
import com.connectrpc.protocols.CONTENT_TYPE
import com.connectrpc.protocols.Envelope
import com.connectrpc.protocols.NetworkProtocol
import io.ktor.http.Url
import okio.Buffer
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

private val UNARY_METHOD_SPEC = MethodSpec(
    path = "",
    requestClass = Any::class,
    responseClass = Any::class,
    streamType = StreamType.UNARY,
)

private val STREAM_METHOD_SPEC = MethodSpec(
    path = "",
    requestClass = Any::class,
    responseClass = Any::class,
    streamType = StreamType.BIDI,
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
            },
        )
        val protocolClientConfig = ProtocolClientConfig(
            host = "host",
            serializationStrategy = mock { },
            networkProtocol = NetworkProtocol.CONNECT,
            interceptors = interceptorFactories,
        )
        unaryChain = protocolClientConfig.createInterceptorChain()
        streamingChain = protocolClientConfig.createStreamingInterceptorChain()
        whenever(protocolClientConfig.serializationStrategy.serializationName()).thenReturn("encoding_type")
    }

    @Test
    fun fifo_request_unary() {
        val response = unaryChain.requestFunction(UnaryHTTPRequest(Url("https://connectrpc.com"), "", null, emptyMap(), UNARY_METHOD_SPEC, Buffer()))
        assertThat(response.headers["id"]).containsExactly("1", "2", "3", "4")
    }

    @Test
    fun lifo_response_unary() {
        val response = unaryChain.responseFunction(HTTPResponse(200, emptyMap(), Buffer(), emptyMap(), null))
        assertThat(response.headers["id"]).containsExactly("4", "3", "2", "1")
    }

    @Test
    fun fifo_request_stream() {
        val request = streamingChain.requestFunction(HTTPRequest(Url("https://connectrpc.com"), "", null, emptyMap(), STREAM_METHOD_SPEC))
        assertThat(request.headers["id"]).containsExactly("1", "2", "3", "4")
    }

    @Test
    fun fifo_request_body() {
        val request = streamingChain.requestBodyFunction(Buffer())
        val (_, unpacked) = Envelope.unpackWithHeaderByte(request)
        assertThat(unpacked.readUtf8()).isEqualTo("1234")
    }

    @Test
    fun lifo_stream_result() {
        val streamResult = streamingChain.streamResultFunction(
            StreamResult.Headers(
                mapOf(CONTENT_TYPE to listOf("application/connect+encoding_type")),
            ),
        ) as StreamResult.Headers
        assertThat(streamResult.headers["id"]).containsExactly("4", "3", "2", "1")
    }

    private class SimpleInterceptor(val id: String) : Interceptor {
        override fun unaryFunction(): UnaryFunction {
            return UnaryFunction(
                requestFunction = {
                    val headers = it.headers.toMutableMap()
                    val sequence = headers["id"]?.toMutableList() ?: mutableListOf()
                    sequence.add(id)
                    headers["id"] = sequence
                    it.clone(headers = headers)
                },
                responseFunction = {
                    val headers = it.headers.toMutableMap()
                    val sequence = headers["id"]?.toMutableList() ?: mutableListOf()
                    sequence.add(id)
                    headers["id"] = sequence
                    it.clone(headers = headers)
                },
            )
        }

        override fun streamFunction(): StreamFunction {
            return StreamFunction(
                requestFunction = {
                    val headers = it.headers.toMutableMap()
                    val sequence = headers["id"]?.toMutableList() ?: mutableListOf()
                    sequence.add(id)
                    headers["id"] = sequence
                    it.clone(headers = headers)
                },
                requestBodyFunction = {
                    it.writeString(id, Charsets.UTF_8)
                },
                streamResultFunction = { result ->
                    result.fold(
                        onHeaders = {
                            val headers = it.headers.toMutableMap()
                            val sequence = headers["id"]?.toMutableList() ?: mutableListOf()
                            sequence.add(id)
                            headers["id"] = sequence
                            StreamResult.Headers(headers)
                        },
                        onMessage = { it },
                        onCompletion = { it },
                    )
                },
            )
        }
    }
}

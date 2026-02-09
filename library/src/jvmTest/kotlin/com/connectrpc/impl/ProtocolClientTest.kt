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

package com.connectrpc.impl

import com.connectrpc.Codec
import com.connectrpc.MethodSpec
import com.connectrpc.ProtocolClientConfig
import com.connectrpc.SerializationStrategy
import com.connectrpc.StreamType
import com.connectrpc.http.HTTPClientInterface
import com.connectrpc.http.HTTPRequest
import com.connectrpc.http.UnaryHTTPRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okio.Buffer
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class ProtocolClientTest {
    private val serializationStrategy: SerializationStrategy = mock { }
    private val codec: Codec<String> = mock { }
    private val httpClient: HTTPClientInterface = mock { }

    @Test
    fun urlConfigurationHostWithTrailingSlashUnary() {
        whenever(codec.encodingName()).thenReturn("testing")
        whenever(codec.serialize(any())).thenReturn(Buffer())
        whenever(serializationStrategy.codec<String>(any())).thenReturn(codec)

        val client = createClient("https://connectrpc.com/")
        client.unary(
            "input",
            emptyMap(),
            createMethodSpec(StreamType.UNARY),
        ) { _ -> }
        val captor = argumentCaptor<UnaryHTTPRequest>()
        verify(httpClient).unary(captor.capture(), any())
        assertThat(captor.firstValue.url.toString()).isEqualTo("https://connectrpc.com/com.connectrpc.SomeService/Service")
    }

    @Test
    fun urlConfigurationHostWithoutTrailingSlashUnary() {
        whenever(codec.encodingName()).thenReturn("testing")
        whenever(codec.serialize(any())).thenReturn(Buffer())
        whenever(serializationStrategy.codec<String>(any())).thenReturn(codec)

        val client = createClient("https://connectrpc.com")
        client.unary(
            "input",
            emptyMap(),
            createMethodSpec(StreamType.UNARY),
        ) { _ -> }
        val captor = argumentCaptor<UnaryHTTPRequest>()
        verify(httpClient).unary(captor.capture(), any())
        assertThat(captor.firstValue.url.toString()).isEqualTo("https://connectrpc.com/com.connectrpc.SomeService/Service")
    }

    @Test
    fun urlConfigurationHostWithTrailingSlashStreaming() {
        whenever(codec.encodingName()).thenReturn("testing")
        whenever(codec.serialize(any())).thenReturn(Buffer())
        whenever(serializationStrategy.codec<String>(any())).thenReturn(codec)

        val client = createClient("https://connectrpc.com/")
        CoroutineScope(Dispatchers.IO).launch {
            client.stream(
                emptyMap(),
                createMethodSpec(StreamType.BIDI),
            )
            val captor = argumentCaptor<UnaryHTTPRequest>()
            verify(httpClient).stream(captor.capture(), true, any())
            assertThat(captor.firstValue.url.toString()).isEqualTo("https://connectrpc.com/com.connectrpc.SomeService/Service")
        }
    }

    @Test
    fun urlConfigurationHostWithoutTrailingSlashStreaming() {
        whenever(codec.encodingName()).thenReturn("testing")
        whenever(codec.serialize(any())).thenReturn(Buffer())
        whenever(serializationStrategy.codec<String>(any())).thenReturn(codec)

        val client = createClient("https://connectrpc.com")
        CoroutineScope(Dispatchers.IO).launch {
            client.stream(
                emptyMap(),
                createMethodSpec(StreamType.BIDI),
            )
            val captor = argumentCaptor<HTTPRequest>()
            verify(httpClient).stream(captor.capture(), true, any())
            assertThat(captor.firstValue.url.toString()).isEqualTo("https://connectrpc.com/com.connectrpc.SomeService/Service")
        }
    }

    @Test
    fun finalUrlIsValid() {
        whenever(codec.encodingName()).thenReturn("testing")
        whenever(codec.serialize(any())).thenReturn(Buffer())
        whenever(serializationStrategy.codec<String>(any())).thenReturn(codec)
        val client = createClient("https://connectrpc.com")
        client.unary(
            "",
            emptyMap(),
            createMethodSpec(StreamType.UNARY),
        ) {}
        val captor = argumentCaptor<UnaryHTTPRequest>()
        verify(httpClient).unary(captor.capture(), any())
        assertThat(captor.firstValue.url.toString()).isEqualTo("https://connectrpc.com/com.connectrpc.SomeService/Service")
    }

    @Test
    fun finalUrlIsValidWithHostEndingInSlash() {
        whenever(codec.encodingName()).thenReturn("testing")
        whenever(codec.serialize(any())).thenReturn(Buffer())
        whenever(serializationStrategy.codec<String>(any())).thenReturn(codec)
        val client = createClient("https://connectrpc.com/")
        client.unary(
            "",
            emptyMap(),
            createMethodSpec(StreamType.UNARY),
        ) {}
        val captor = argumentCaptor<UnaryHTTPRequest>()
        verify(httpClient).unary(captor.capture(), any())
        assertThat(captor.firstValue.url.toString()).isEqualTo("https://connectrpc.com/com.connectrpc.SomeService/Service")
    }

    @Test
    fun finalUrlRelativeBaseURI() {
        whenever(codec.encodingName()).thenReturn("testing")
        whenever(codec.serialize(any())).thenReturn(Buffer())
        whenever(serializationStrategy.codec<String>(any())).thenReturn(codec)
        val client = createClient("https://connectrpc.com/api")
        client.unary(
            "",
            emptyMap(),
            createMethodSpec(StreamType.UNARY),
        ) {}
        val captor = argumentCaptor<UnaryHTTPRequest>()
        verify(httpClient).unary(captor.capture(), any())
        assertThat(captor.firstValue.url.toString()).isEqualTo("https://connectrpc.com/api/com.connectrpc.SomeService/Service")
    }

    @Test
    fun finalUrlAbsoluteBaseURI() {
        whenever(codec.encodingName()).thenReturn("testing")
        whenever(codec.serialize(any())).thenReturn(Buffer())
        whenever(serializationStrategy.codec<String>(any())).thenReturn(codec)
        val client = createClient("https://connectrpc.com/api/")
        client.unary(
            "",
            emptyMap(),
            createMethodSpec(StreamType.UNARY),
        ) {}
        val captor = argumentCaptor<UnaryHTTPRequest>()
        verify(httpClient).unary(captor.capture(), any())
        assertThat(captor.firstValue.url.toString()).isEqualTo("https://connectrpc.com/api/com.connectrpc.SomeService/Service")
    }

    private fun createClient(host: String): ProtocolClient {
        return ProtocolClient(
            httpClient = httpClient,
            config = ProtocolClientConfig(
                host = host,
                serializationStrategy = serializationStrategy,
            ),
        )
    }

    private fun createMethodSpec(streamType: StreamType): MethodSpec<String, String> {
        return MethodSpec(
            path = "com.connectrpc.SomeService/Service",
            String::class,
            String::class,
            streamType,
        )
    }
}

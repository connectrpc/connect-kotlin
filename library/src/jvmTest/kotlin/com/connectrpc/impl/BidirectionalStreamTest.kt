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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okio.Buffer
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.lang.IllegalArgumentException

class BidirectionalStreamTest {
    private val serializationStrategy: SerializationStrategy = mock { }
    private val codec: Codec<String> = mock { }

    @Test
    fun sendOnCloseReturnsFailureResult() {
        whenever(codec.encodingName()).thenReturn("testing")
        whenever(codec.serialize(any())).thenReturn(Buffer())
        whenever(serializationStrategy.codec<String>(any())).thenReturn(codec)

        val client = ProtocolClient(
            httpClient = mock { },
            config = ProtocolClientConfig(
                host = "https://connectrpc.com/",
                serializationStrategy = serializationStrategy,
            ),
        )

        CoroutineScope(Dispatchers.IO).launch {
            val stream = client.stream(
                emptyMap(),
                MethodSpec(
                    path = "com.connectrpc.SomeService/Service",
                    String::class,
                    String::class,
                    streamType = StreamType.BIDI,
                ),
            )
            stream.sendClose()
            val result = stream.send("input")
            assertThat(result.isFailure).isTrue()
        }
    }

    @Test
    fun sendWithSerializingErrorReturnsFailureResult() {
        whenever(codec.encodingName()).thenReturn("testing")
        whenever(codec.serialize(any())).thenThrow(IllegalArgumentException("testing"))
        whenever(serializationStrategy.codec<String>(any())).thenReturn(codec)

        val client = ProtocolClient(
            httpClient = mock { },
            config = ProtocolClientConfig(
                host = "https://connectrpc.com/",
                serializationStrategy = serializationStrategy,
            ),
        )

        CoroutineScope(Dispatchers.IO).launch {
            val stream = client.stream(
                emptyMap(),
                MethodSpec(
                    path = "com.connectrpc.SomeService/Service",
                    String::class,
                    String::class,
                    streamType = StreamType.BIDI,
                ),
            )
            val result = stream.send("input")
            assertThat(result.isFailure).isTrue()
            assertThat(result.exceptionOrNull()).isEqualTo(IllegalArgumentException("testing"))
            stream.sendClose()
        }
    }
}

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

package build.buf.connect.impl

import build.buf.connect.Codec
import build.buf.connect.MethodSpec
import build.buf.connect.ProtocolClientConfig
import build.buf.connect.SerializationStrategy
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

class BiDirectionalStreamTest {
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
                serializationStrategy = serializationStrategy
            )
        )

        CoroutineScope(Dispatchers.IO).launch {
            val stream = client.stream(
                emptyMap(),
                MethodSpec(
                    path = "build.buf.connect.SomeService/Service",
                    String::class,
                    String::class
                )
            )

            stream.close()
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
                serializationStrategy = serializationStrategy
            )
        )

        CoroutineScope(Dispatchers.IO).launch {
            val stream = client.stream(
                emptyMap(),
                MethodSpec(
                    path = "build.buf.connect.SomeService/Service",
                    String::class,
                    String::class
                )
            )

            stream.close()
            val result = stream.send("input")
            assertThat(result.isFailure).isTrue()
            assertThat(result.exceptionOrNull()).isEqualTo(IllegalArgumentException("testing"))
        }
    }
}

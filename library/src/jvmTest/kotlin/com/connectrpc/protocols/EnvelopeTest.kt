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

package com.connectrpc.protocols

import com.connectrpc.compression.GzipCompressionPool
import okio.Buffer
import okio.ByteString.Companion.encodeUtf8
import okio.internal.commonAsUtf8ToByteArray
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class EnvelopeTest {
    @Test
    fun noCompression() {
        val enveloped = Envelope.pack(Buffer().write("hello_world".encodeUtf8()))
        val (_, unpacked) = Envelope.unpackWithHeaderByte(enveloped)
        assertThat(unpacked.readUtf8()).isEqualTo("hello_world")
    }

    @Test
    fun compression() {
        val enveloped = Envelope.pack(Buffer().write("hello_world".encodeUtf8()), GzipCompressionPool, 1)
        val (_, unpacked) = Envelope.unpackWithHeaderByte(enveloped, GzipCompressionPool)
        assertThat(unpacked.readUtf8()).isEqualTo("hello_world")
    }

    @Test
    fun unpackWithHeaderByte() {
        val payload = "hello_world".commonAsUtf8ToByteArray()
        val enveloped = Buffer()
            .writeByte(100)
            .writeInt(payload.size)
            .write(payload)
        val (headerByte, unpacked) = Envelope.unpackWithHeaderByte(enveloped, GzipCompressionPool)
        assertThat(unpacked.readUtf8()).isEqualTo("hello_world")
        assertThat(headerByte).isEqualTo(100)
    }
}

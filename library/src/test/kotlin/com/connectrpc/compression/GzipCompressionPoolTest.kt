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

package com.connectrpc.compression

import okio.Buffer
import okio.ByteString.Companion.encodeUtf8
import okio.GzipSink
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import kotlin.random.Random

class GzipCompressionPoolTest {

    @Test
    fun gzipCompressionDecompression() {
        val rand = Random.Default
        val compressionPool = GzipCompressionPool
        for (i in 1..10_000) {
            val inputLen = rand.nextInt(200, 2000)
            val input = rand.nextBytes(inputLen)
            val buffer = Buffer().write(input)
            val compressed = compressionPool.compress(buffer)
            val result = compressionPool.decompress(compressed)
            assertThat(result.readByteArray()).isEqualTo(input)
        }
    }

    @Test
    fun ensureGzipDecompression() {
        val compressionPool = GzipCompressionPool
        val buffer = Buffer().write("some_string".encodeUtf8())
        val result = Buffer()
        GzipSink(result).use { source ->
            source.write(buffer, buffer.size)
        }
        val resultString = compressionPool.decompress(result).readUtf8()
        assertThat(resultString).isEqualTo("some_string")
    }

    @Test
    fun emptyBufferGzipDecompression() {
        val compressionPool = GzipCompressionPool
        val resultString = compressionPool.decompress(Buffer()).readUtf8()
        assertThat(resultString).isEqualTo("")
    }
}

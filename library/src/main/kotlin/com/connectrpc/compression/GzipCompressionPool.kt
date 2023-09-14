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

package com.connectrpc.compression

import okio.Buffer
import okio.GzipSink
import okio.GzipSource

/**
 * Provides an implementation of gzip for encoding/decoding, allowing the client to compress
 * and decompress requests/responses using gzip.
 *
 */
object GzipCompressionPool : CompressionPool {
    override fun name(): String {
        return "gzip"
    }

    override fun compress(buffer: Buffer): Buffer {
        val gzippedSink = Buffer()
        GzipSink(gzippedSink).use { source ->
            source.write(buffer, buffer.size)
        }
        return gzippedSink
    }

    override fun decompress(buffer: Buffer): Buffer {
        val result = Buffer()
        val source = GzipSource(buffer)
        while (source.read(result, Int.MAX_VALUE.toLong()) != -1L) {
            // continue reading.
        }
        return result
    }
}

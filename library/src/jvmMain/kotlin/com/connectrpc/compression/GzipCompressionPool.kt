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

    override fun compress(input: Buffer): Buffer {
        val result = Buffer()
        GzipSink(result).use { gzippedSink ->
            gzippedSink.write(input, input.size)
        }
        return result
    }

    override fun decompress(input: Buffer): Buffer {
        val result = Buffer()
        // We're lenient and will allow an empty payload to be
        // interpreted as a compressed empty payload (even though
        // it's missing the gzip format preamble/metadata).
        if (input.size == 0L) return result

        GzipSource(input).use { gzippedSource ->
            while (gzippedSource.read(result, Int.MAX_VALUE.toLong()) != -1L) {
                // continue reading.
            }
        }
        return result
    }
}

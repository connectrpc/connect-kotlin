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

import com.connectrpc.compression.CompressionPool
import okio.Buffer

/**
 * Configuration used to specify if/how requests should be compressed.
 */
data class RequestCompression(
    // The minimum number of bytes that a request message should be for compression to be used.
    val minBytes: Int,
    // The compression pool that should be used for compressing outbound requests.
    val compressionPool: CompressionPool,
) {
    /**
     * Checks if the input buffer meets the compression requirements.
     */
    fun shouldCompress(buffer: Buffer): Boolean {
        return buffer.size >= minBytes
    }
}

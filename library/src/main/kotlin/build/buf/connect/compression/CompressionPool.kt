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

package build.buf.connect.compression

import okio.Buffer

/**
 * Conforming types provide the functionality to compress/decompress data using a specific
 * algorithm.
 *
 * [build.buf.connect.ProtocolClientInterface] implementations are expected to use the first compression pool with
 * a matching [name] for decompressing inbound responses.
 *
 * Outbound request compression can be specified using additional options that specify a
 * `compressionName` that matches a compression pool's [name].
 */
interface CompressionPool {
    /**
     * The name of the compression pool, which corresponds to the `content-encoding` header.
     * Example: `gzip`.
     * @return The name of the compression pool that can be used with the `content-encoding`
     * header.
     */
    fun name(): String

    /**
     * Compress an outbound request message.
     * @param buffer: The uncompressed request message.
     * @return The compressed request message.
     */
    fun compress(buffer: Buffer): Buffer

    /**
     * Decompress an inbound response message.
     * @param buffer: The compressed response message.
     * @return The uncompressed response message.
     */
    fun decompress(buffer: Buffer): Buffer
}

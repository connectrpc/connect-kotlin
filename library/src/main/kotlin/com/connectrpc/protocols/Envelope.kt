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

package com.connectrpc.protocols

import com.connectrpc.compression.CompressionPool
import okio.Buffer

/**
 * Provides functionality for packing and unpacking (headers and length prefixed) messages.
 */
class Envelope {
    companion object {
        /**
         * Compresses the source buffer with the specified compression pool and then envelopes the data.
         *
         * @param source The source buffer containing data to be compressed.
         * @param compressionPool The compression pool to be used.
         * @param compressionMinBytes The minimum bytes the source needs to be in order to be compressed.
         */
        fun pack(source: Buffer, compressionPool: CompressionPool? = null, compressionMinBytes: Int? = null): Buffer {
            if (compressionMinBytes == null ||
                source.size < compressionMinBytes ||
                compressionPool == null
            ) {
                return source.use {
                    val result = Buffer()
                    result.writeByte(0)
                    result.writeInt(source.buffer.size.toInt())
                    result.writeAll(source)
                    result
                }
            }
            return source.use { buffer ->
                val result = Buffer()
                result.writeByte(1)
                val compressedBuffer = compressionPool.compress(buffer)
                result.writeInt(compressedBuffer.size.toInt())
                result.writeAll(compressedBuffer)
                result
            }
        }

        /**
         * The unpackWithHeaderByte will read the first enveloped message in the source buffer and return it.
         * Any remaining bytes in the source buffer is left alone.
         *
         * @param source The source buffer containing the enveloped message.
         * @param compressionPool The compression pool used to decompress the payload.
         *
         * @return The header byte along with the resulting data. The header byte is used to communicate flags.
         */
        fun unpackWithHeaderByte(source: Buffer, compressionPool: CompressionPool? = null): Pair<Int, Buffer> {
            return source.use { bufferSource ->
                val result = Buffer()
                if (bufferSource.exhausted()) {
                    return@use 0 to result
                }
                val headerByte = bufferSource.readByte().toInt()
                val length = bufferSource.readInt().toLong()
                val message = Buffer()
                bufferSource.read(message, length)
                val decompressSource = when (headerByte.and(1)) {
                    1 -> compressionPool?.decompress(message) ?: message
                    else -> message
                }
                result.writeAll(decompressSource)
                headerByte to result
            }
        }
    }
}

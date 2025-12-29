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

import com.connectrpc.Code
import com.connectrpc.ConnectException
import com.connectrpc.compression.CompressionPool
import okio.Buffer
import okio.Source

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
            val flags: Int
            val payload: Buffer
            if (compressionMinBytes == null ||
                source.size < compressionMinBytes ||
                compressionPool == null
            ) {
                flags = 0
                payload = source
            } else {
                flags = 1
                payload = compressionPool.compress(source)
            }
            val result = Buffer()
            result.writeByte(flags)
            result.writeInt(payload.size.toInt())
            result.writeAll(payload)
            return result
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
            if (source.exhausted()) {
                return 0 to source
            }
            val headerByte = source.readByte().toInt()
            val length = source.readInt().toLong()
            val message = if (source.size >= length) {
                // extract relevant subset for this message
                Buffer().write(source as Source, length)
            } else {
                throw ConnectException(
                    code = Code.INTERNAL_ERROR,
                    message = "stream message was incomplete: expecting $length bytes, got ${source.size}",
                )
            }
            return headerByte to when (headerByte.and(1)) {
                1 -> {
                    if (compressionPool == null) {
                        throw ConnectException(
                            code = Code.INTERNAL_ERROR,
                            message = "stream message was compressed but no known encoding",
                        )
                    }
                    compressionPool.decompress(message)
                }

                else -> {
                    message
                }
            }
        }
    }
}

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

package com.connectrpc.server.protocols

import com.connectrpc.Code
import com.connectrpc.ConnectErrorDetail
import okio.Buffer
import okio.ByteString.Companion.encodeUtf8

/**
 * Encodes error details as google.rpc.Status protobuf message.
 *
 * This is a manual implementation to avoid depending on protobuf-java.
 *
 * google.rpc.Status proto:
 * ```protobuf
 * message Status {
 *   int32 code = 1;
 *   string message = 2;
 *   repeated google.protobuf.Any details = 3;
 * }
 * ```
 *
 * google.protobuf.Any proto:
 * ```protobuf
 * message Any {
 *   string type_url = 1;
 *   bytes value = 2;
 * }
 * ```
 */
internal object StatusProtoEncoder {

    // Wire type constants
    private const val WIRETYPE_VARINT = 0
    private const val WIRETYPE_LENGTH_DELIMITED = 2

    /**
     * Encodes a gRPC error status with details as a protobuf message.
     *
     * @param code The gRPC status code.
     * @param message The error message.
     * @param details The error details.
     * @return The encoded protobuf bytes.
     */
    fun encode(code: Code, message: String?, details: List<ConnectErrorDetail>): ByteArray {
        val buffer = Buffer()

        // Field 1: int32 code
        writeTag(buffer, 1, WIRETYPE_VARINT)
        writeVarint(buffer, code.value.toLong())

        // Field 2: string message
        if (!message.isNullOrEmpty()) {
            writeTag(buffer, 2, WIRETYPE_LENGTH_DELIMITED)
            writeString(buffer, message)
        }

        // Field 3: repeated Any details
        for (detail in details) {
            writeTag(buffer, 3, WIRETYPE_LENGTH_DELIMITED)
            val anyBytes = encodeAny(detail)
            writeVarint(buffer, anyBytes.size.toLong())
            buffer.write(anyBytes)
        }

        return buffer.readByteArray()
    }

    /**
     * Encodes a ConnectErrorDetail as google.protobuf.Any.
     */
    private fun encodeAny(detail: ConnectErrorDetail): ByteArray {
        val buffer = Buffer()

        // Field 1: string type_url
        writeTag(buffer, 1, WIRETYPE_LENGTH_DELIMITED)
        writeString(buffer, detail.type)

        // Field 2: bytes value
        val payload = detail.payload.toByteArray()
        if (payload.isNotEmpty()) {
            writeTag(buffer, 2, WIRETYPE_LENGTH_DELIMITED)
            writeVarint(buffer, payload.size.toLong())
            buffer.write(payload)
        }

        return buffer.readByteArray()
    }

    /**
     * Writes a protobuf field tag.
     */
    private fun writeTag(buffer: Buffer, fieldNumber: Int, wireType: Int) {
        writeVarint(buffer, ((fieldNumber shl 3) or wireType).toLong())
    }

    /**
     * Writes a variable-length integer (varint).
     */
    private fun writeVarint(buffer: Buffer, value: Long) {
        var v = value
        while (v and 0x7FL.inv() != 0L) {
            buffer.writeByte(((v.toInt() and 0x7F) or 0x80))
            v = v ushr 7
        }
        buffer.writeByte(v.toInt())
    }

    /**
     * Writes a length-delimited string.
     */
    private fun writeString(buffer: Buffer, value: String) {
        val bytes = value.encodeUtf8()
        writeVarint(buffer, bytes.size.toLong())
        buffer.write(bytes)
    }
}

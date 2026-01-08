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
import okio.ByteString.Companion.decodeBase64
import okio.ByteString.Companion.toByteString
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class StatusProtoEncoderTest {

    @Test
    fun `encodes status with code only`() {
        val bytes = StatusProtoEncoder.encode(
            code = Code.NOT_FOUND,
            message = null,
            details = emptyList(),
        )

        // Verify it's valid protobuf
        assertThat(bytes).isNotEmpty()

        // Field 1 (code): tag=0x08 (field 1, varint), value=5 (NOT_FOUND)
        assertThat(bytes[0]).isEqualTo(0x08.toByte()) // tag
        assertThat(bytes[1]).isEqualTo(0x05.toByte()) // value = 5
    }

    @Test
    fun `encodes status with code and message`() {
        val bytes = StatusProtoEncoder.encode(
            code = Code.INVALID_ARGUMENT,
            message = "test error",
            details = emptyList(),
        )

        assertThat(bytes).isNotEmpty()

        // Field 1 (code): tag=0x08, value=3 (INVALID_ARGUMENT)
        assertThat(bytes[0]).isEqualTo(0x08.toByte())
        assertThat(bytes[1]).isEqualTo(0x03.toByte())

        // Field 2 (message): tag=0x12 (field 2, length-delimited)
        assertThat(bytes[2]).isEqualTo(0x12.toByte())

        // The message "test error" should be in the bytes
        val encoded = bytes.toByteString().utf8()
        assertThat(encoded).contains("test error")
    }

    @Test
    fun `encodes status with details`() {
        val detail = ConnectErrorDetail(
            type = "type.googleapis.com/google.rpc.BadRequest",
            payload = "test payload".encodeToByteArray().toByteString(),
        )

        val bytes = StatusProtoEncoder.encode(
            code = Code.INVALID_ARGUMENT,
            message = "invalid request",
            details = listOf(detail),
        )

        assertThat(bytes).isNotEmpty()

        // Should contain the type URL
        val encoded = bytes.toByteString().utf8()
        assertThat(encoded).contains("type.googleapis.com/google.rpc.BadRequest")
    }

    @Test
    fun `encodes status with multiple details`() {
        val detail1 = ConnectErrorDetail(
            type = "type.googleapis.com/google.rpc.BadRequest",
            payload = "payload1".encodeToByteArray().toByteString(),
        )
        val detail2 = ConnectErrorDetail(
            type = "type.googleapis.com/google.rpc.DebugInfo",
            payload = "payload2".encodeToByteArray().toByteString(),
        )

        val bytes = StatusProtoEncoder.encode(
            code = Code.INTERNAL_ERROR,
            message = "multiple errors",
            details = listOf(detail1, detail2),
        )

        assertThat(bytes).isNotEmpty()

        val encoded = bytes.toByteString().utf8()
        assertThat(encoded).contains("BadRequest")
        assertThat(encoded).contains("DebugInfo")
    }

    @Test
    fun `encoded bytes can be base64 encoded for trailer`() {
        val bytes = StatusProtoEncoder.encode(
            code = Code.PERMISSION_DENIED,
            message = "access denied",
            details = emptyList(),
        )

        val base64 = bytes.toByteString().base64()
        assertThat(base64).isNotEmpty()

        // Should be decodable
        val decoded = base64.decodeBase64()
        assertThat(decoded).isNotNull()
        assertThat(decoded!!.toByteArray()).isEqualTo(bytes)
    }

    @Test
    fun `encodes empty message correctly`() {
        val bytes = StatusProtoEncoder.encode(
            code = Code.CANCELED, // Code 1
            message = "",
            details = emptyList(),
        )

        // Should only have the code field
        assertThat(bytes.size).isEqualTo(2) // tag + value
    }

    @Test
    fun `encodes unicode message correctly`() {
        val bytes = StatusProtoEncoder.encode(
            code = Code.INVALID_ARGUMENT,
            message = "エラーが発生しました",
            details = emptyList(),
        )

        val encoded = bytes.toByteString().utf8()
        assertThat(encoded).contains("エラーが発生しました")
    }

    @Test
    fun `encodes large varint correctly`() {
        // Internal error has code 13, which still fits in one byte
        val bytes = StatusProtoEncoder.encode(
            code = Code.INTERNAL_ERROR,
            message = null,
            details = emptyList(),
        )

        assertThat(bytes[0]).isEqualTo(0x08.toByte()) // tag
        assertThat(bytes[1]).isEqualTo(0x0D.toByte()) // value = 13
    }
}

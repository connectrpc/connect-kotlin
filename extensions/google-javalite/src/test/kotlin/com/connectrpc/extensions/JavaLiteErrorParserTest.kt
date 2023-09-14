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

package com.connectrpc.extensions

import com.connectrpc.AnyError
import com.connectrpc.ConnectErrorDetail
import com.connectrpc.google.rpc.Status
import com.google.protobuf.ByteString
import okio.ByteString.Companion.encodeUtf8
import okio.ByteString.Companion.toByteString
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class JavaLiteErrorParserTest {
    private val parser = JavaLiteErrorParser

    @Test
    fun unpackingProto() {
        val proto = Status.newBuilder()
            .setCode(123)
            .setMessage("hello")
            .build()
        val serializedError = AnyError(
            "type.googleapis.com/com.connectrpc.google.rpc.Status",
            proto.toByteArray().toByteString().base64().encodeUtf8()
        )
        val unpacked = parser.unpack(serializedError, Status::class)
        assertThat(unpacked!!.code).isEqualTo(123)
        assertThat(unpacked.message).isEqualTo("hello")
    }

    @Test
    fun detailParsingEmptyDetails() {
        val proto = Status.newBuilder()
            .setCode(123)
            .setMessage("hello")
            .build()
        val details = parser.parseDetails(proto.toByteArray())
        assertThat(details).isEmpty()
    }

    @Test
    fun detailsParsingManyDetails() {
        val proto = Status.newBuilder()
            .setCode(123)
            .setMessage("hello")
            .addDetails(
                com.google.protobuf.Any.newBuilder()
                    .setTypeUrl("any_message_1")
                    .setValue(ByteString.copyFrom("value_1".encodeUtf8().base64().encodeToByteArray()))
                    .build()
            )
            .addDetails(
                com.google.protobuf.Any.newBuilder()
                    .setTypeUrl("any_message_2")
                    .setValue(ByteString.copyFrom("value_2".encodeUtf8().base64().encodeToByteArray()))
                    .build()
            )
            .build()
        val details = parser.parseDetails(proto.toByteArray())
        assertThat(details).containsExactly(ConnectErrorDetail("any_message_1", "value_1".encodeUtf8()), ConnectErrorDetail("any_message_2", "value_2".encodeUtf8()))
    }
}

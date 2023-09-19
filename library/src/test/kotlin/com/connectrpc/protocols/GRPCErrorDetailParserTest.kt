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

package com.connectrpc.protocols

import com.connectrpc.Code
import com.connectrpc.ErrorDetailParser
import okio.ByteString.Companion.encodeUtf8
import okio.internal.commonAsUtf8ToByteArray
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class GRPCErrorDetailParserTest {

    private val errorDetailParser: ErrorDetailParser = mock { }

    @Test
    fun trailers() {
        val parser = GRPCCompletionParser(errorDetailParser)
        val completion = parser.parse(
            headers = emptyMap(),
            trailers = mapOf(
                GRPC_STATUS_TRAILER to listOf("${Code.UNAUTHENTICATED.value}"),
                GRPC_MESSAGE_TRAILER to listOf("str"),
                GRPC_STATUS_DETAILS_TRAILERS to listOf("data".encodeUtf8().base64())
            )
        )
        assertThat(completion!!.code).isEqualTo(Code.UNAUTHENTICATED)
        assertThat(completion.message.utf8()).isEqualTo("str")
        verify(errorDetailParser).parseDetails("data".commonAsUtf8ToByteArray())
    }

    @Test
    fun trailersWithoutStatus() {
        val parser = GRPCCompletionParser(errorDetailParser)
        val completion = parser.parse(
            headers = emptyMap(),
            trailers = mapOf(
                GRPC_MESSAGE_TRAILER to listOf("str"),
                GRPC_STATUS_DETAILS_TRAILERS to listOf("data".encodeUtf8().base64())
            )
        )
        assertThat(completion).isNull()
    }

    @Test
    fun trailersWithoutMessage() {
        val parser = GRPCCompletionParser(errorDetailParser)
        val completion = parser.parse(
            headers = mapOf(
                GRPC_STATUS_TRAILER to listOf("${Code.UNAUTHENTICATED.value}"),
                GRPC_STATUS_DETAILS_TRAILERS to listOf("data".encodeUtf8().base64())
            ),
            trailers = emptyMap()
        )
        assertThat(completion!!.code).isEqualTo(Code.UNAUTHENTICATED)
    }

    @Test
    fun trailersWithoutErrorDetails() {
        val parser = GRPCCompletionParser(errorDetailParser)
        val completion = parser.parse(
            headers = emptyMap(),
            trailers = mapOf(
                GRPC_STATUS_TRAILER to listOf("${Code.UNAUTHENTICATED.value}"),
                GRPC_MESSAGE_TRAILER to listOf("str")
            )
        )
        assertThat(completion!!.errorDetails).isEmpty()
    }
}

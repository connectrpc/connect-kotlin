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
import com.connectrpc.ErrorDetailParser
import okio.ByteString.Companion.encodeUtf8
import okio.internal.commonAsUtf8ToByteArray
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions

class GRPCErrorDetailParserTest {

    private val errorDetailParser: ErrorDetailParser = mock { }

    @Test
    fun trailers() {
        val parser = GRPCCompletionParser(errorDetailParser)
        val completion = parser.parse(
            headers = emptyMap(),
            hasBody = false,
            trailers = mapOf(
                GRPC_STATUS_TRAILER to listOf("${Code.UNAUTHENTICATED.value}"),
                GRPC_MESSAGE_TRAILER to listOf("str"),
                GRPC_STATUS_DETAILS_TRAILERS to listOf("data".encodeUtf8().base64()),
            ),
        )
        assertThat(completion.present).isTrue()
        assertThat(completion.code).isEqualTo(Code.UNAUTHENTICATED)
        assertThat(completion.message).isEqualTo("str")
        verify(errorDetailParser).parseDetails("data".commonAsUtf8ToByteArray())
    }

    @Test
    fun trailersWithoutStatus() {
        val parser = GRPCCompletionParser(errorDetailParser)
        val completion = parser.parse(
            headers = emptyMap(),
            hasBody = false,
            trailers = mapOf(
                GRPC_MESSAGE_TRAILER to listOf("str"),
                GRPC_STATUS_DETAILS_TRAILERS to listOf("data".encodeUtf8().base64()),
            ),
        )
        assertThat(completion.present).isFalse()
        assertThat(completion.code).isEqualTo(Code.UNKNOWN)
        assertThat(completion.errorDetails).isEmpty()
        verifyNoInteractions(errorDetailParser)
    }

    @Test
    fun trailersOnly() {
        val parser = GRPCCompletionParser(errorDetailParser)
        val completion = parser.parse(
            headers = mapOf(
                GRPC_STATUS_TRAILER to listOf("${Code.UNAUTHENTICATED.value}"),
                GRPC_MESSAGE_TRAILER to listOf("str"),
                GRPC_STATUS_DETAILS_TRAILERS to listOf("data".encodeUtf8().base64()),
            ),
            hasBody = false,
            trailers = emptyMap(),
        )
        assertThat(completion.present).isTrue()
        assertThat(completion.code).isEqualTo(Code.UNAUTHENTICATED)
        assertThat(completion.message).isEqualTo("str")
        verify(errorDetailParser).parseDetails("data".commonAsUtf8ToByteArray())
    }

    @Test
    fun trailersWithoutStatusIncorrectStatusInHeaders() {
        val parser = GRPCCompletionParser(errorDetailParser)
        val completion = parser.parse(
            headers = mapOf(
                GRPC_STATUS_TRAILER to listOf("${Code.UNAUTHENTICATED.value}"),
                GRPC_MESSAGE_TRAILER to listOf("str"),
                GRPC_STATUS_DETAILS_TRAILERS to listOf("data".encodeUtf8().base64()),
            ),
            // since there is a body, we don't look for a status in the headers
            // because it a trailers-only response has no body and no trailers
            hasBody = true,
            trailers = emptyMap(),
        )
        assertThat(completion.present).isFalse()
        assertThat(completion.code).isEqualTo(Code.UNKNOWN)
        assertThat(completion.errorDetails).isEmpty()
        verifyNoInteractions(errorDetailParser)
    }

    @Test
    fun trailersWithoutMessage() {
        val parser = GRPCCompletionParser(errorDetailParser)
        val completion = parser.parse(
            headers = emptyMap(),
            hasBody = false,
            trailers = mapOf(
                GRPC_STATUS_TRAILER to listOf("${Code.UNAUTHENTICATED.value}"),
                GRPC_STATUS_DETAILS_TRAILERS to listOf("data".encodeUtf8().base64()),
            ),
        )
        assertThat(completion.present).isTrue()
        assertThat(completion.code).isEqualTo(Code.UNAUTHENTICATED)
        verify(errorDetailParser).parseDetails("data".commonAsUtf8ToByteArray())
    }

    @Test
    fun trailersWithoutErrorDetails() {
        val parser = GRPCCompletionParser(errorDetailParser)
        val completion = parser.parse(
            headers = emptyMap(),
            hasBody = false,
            trailers = mapOf(
                GRPC_STATUS_TRAILER to listOf("${Code.UNAUTHENTICATED.value}"),
                GRPC_MESSAGE_TRAILER to listOf("str"),
            ),
        )
        assertThat(completion.present).isTrue()
        assertThat(completion.message).isEqualTo("str")
        assertThat(completion.errorDetails).isEmpty()
        verifyNoInteractions(errorDetailParser)
    }
}

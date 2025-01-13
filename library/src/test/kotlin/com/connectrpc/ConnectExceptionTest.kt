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

import okio.ByteString.Companion.encodeUtf8
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class ConnectExceptionTest {
    private val errorDetailParser: ErrorDetailParser = mock { }

    @Test
    fun connectErrorParsing() {
        val errorDetail = ConnectErrorDetail(
            "type",
            "value".encodeUtf8(),
        )
        whenever(errorDetailParser.unpack(errorDetail.pb, String::class)).thenReturn("unpacked_value")
        val connectException = ConnectException(
            code = Code.UNKNOWN,
        ).withErrorDetails(
            errorParser = errorDetailParser,
            details = listOf(
                errorDetail,
                errorDetail,
            ),
        )
        val parsedResult = connectException.unpackedDetails(String::class)
        assertThat(parsedResult).contains("unpacked_value", "unpacked_value")
    }
}

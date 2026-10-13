// Copyright 2022-2026 The Connect Authors
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

package com.connectrpc.okhttp

import com.connectrpc.Code
import okhttp3.internal.http2.ErrorCode
import okhttp3.internal.http2.StreamResetException
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.io.IOException
import java.io.InterruptedIOException
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

class CodeFromExceptionTest {
    @Test
    fun ioExceptionAfterDeadlineIsDeadlineExceeded() {
        val deadline = TimeSource.Monotonic.markNow() - 1.seconds
        val code = codeFromException(false, StreamResetException(ErrorCode.CANCEL), deadline)
        assertThat(code).isEqualTo(Code.DEADLINE_EXCEEDED)
    }

    @Test
    fun ioExceptionBeforeDeadlineIsUnknown() {
        val deadline = TimeSource.Monotonic.markNow() + 1.hours
        val code = codeFromException(false, IOException("unexpected end of stream"), deadline)
        assertThat(code).isEqualTo(Code.UNKNOWN)
    }

    @Test
    fun ioExceptionWithoutDeadlineIsUnknown() {
        assertThat(codeFromException(false, IOException("unexpected end of stream"))).isEqualTo(Code.UNKNOWN)
    }

    @Test
    fun ioExceptionOnCanceledCallIsCanceled() {
        assertThat(codeFromException(true, IOException("Canceled"))).isEqualTo(Code.CANCELED)
    }

    @Test
    fun timeoutIsDeadlineExceeded() {
        assertThat(codeFromException(false, InterruptedIOException("timeout"))).isEqualTo(Code.DEADLINE_EXCEEDED)
    }

    @Test
    fun streamResetBeforeDeadlineUsesErrorCode() {
        val deadline = TimeSource.Monotonic.markNow() + 1.hours
        val code = codeFromException(false, StreamResetException(ErrorCode.CANCEL), deadline)
        assertThat(code).isEqualTo(Code.CANCELED)
    }

    @Test
    fun streamResetErrorCodes() {
        val expected = mapOf(
            ErrorCode.NO_ERROR to Code.INTERNAL_ERROR,
            ErrorCode.PROTOCOL_ERROR to Code.INTERNAL_ERROR,
            ErrorCode.INTERNAL_ERROR to Code.INTERNAL_ERROR,
            ErrorCode.FLOW_CONTROL_ERROR to Code.INTERNAL_ERROR,
            ErrorCode.SETTINGS_TIMEOUT to Code.INTERNAL_ERROR,
            ErrorCode.STREAM_CLOSED to Code.UNKNOWN,
            ErrorCode.FRAME_SIZE_ERROR to Code.INTERNAL_ERROR,
            ErrorCode.REFUSED_STREAM to Code.UNAVAILABLE,
            ErrorCode.CANCEL to Code.CANCELED,
            ErrorCode.COMPRESSION_ERROR to Code.INTERNAL_ERROR,
            ErrorCode.CONNECT_ERROR to Code.INTERNAL_ERROR,
            ErrorCode.ENHANCE_YOUR_CALM to Code.RESOURCE_EXHAUSTED,
            ErrorCode.INADEQUATE_SECURITY to Code.PERMISSION_DENIED,
            ErrorCode.HTTP_1_1_REQUIRED to Code.UNKNOWN,
        )
        assertThat(expected.keys).containsExactlyInAnyOrder(*ErrorCode.entries.toTypedArray())
        for ((errorCode, code) in expected) {
            assertThat(codeFromException(false, StreamResetException(errorCode)))
                .describedAs(errorCode.name)
                .isEqualTo(code)
        }
    }
}

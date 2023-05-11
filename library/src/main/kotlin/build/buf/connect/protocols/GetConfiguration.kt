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

package build.buf.connect.protocols

import build.buf.connect.Idempotency
import build.buf.connect.MethodSpec
import okio.Buffer

object GetSupportConstants {
    const val CONNECT_VERSION_QUERY_PARAM_KEY = "connect"
    const val ENCODING_QUERY_PARAM_KEY = "encoding"
    const val BASE64_QUERY_PARAM_KEY = "base64"
    const val MESSAGE_QUERY_PARAM_KEY = "message"
    const val COMPRESSION_QUERY_PARAM_KEY = "compression"
    const val CONNECT_VERSION_QUERY_PARAM_VALUE = "v1"
}

/**
 * Configuration for enabling Get requests for the Connect protocol.
 */
data class GetConfiguration(
    // To enable falling back on the vanilla unary POST when
    // the payload is too large.
    val fallbackEnabled: Boolean = true,
    // The max number of bytes the payload can be before falling
    // back onto the vanilla unary POST.
    val maxUrlBytes: Int = 50_000
) {
    /**
     * Determines if the method is compatible for a GET request.
     *
     * @param methodSpec The method specification of the request.
     *
     * @return true, when the method is compatible for a GET request.
     */
    fun isGetEnabled(methodSpec: MethodSpec<*, *>): Boolean {
        return methodSpec.idempotency == Idempotency.NO_SIDE_EFFECTS
    }

    /**
     * Determines if the input buffer is eligible for a POST fallback.
     *
     * @param buffer The request payload.
     *
     * @return true, if the request is larger than the set threshold.
     */
    fun useFallback(buffer: Buffer): Boolean {
        return fallbackEnabled && maxUrlBytes < buffer.size
    }
}

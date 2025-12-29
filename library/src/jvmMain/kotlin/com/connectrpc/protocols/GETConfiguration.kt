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

import okio.Buffer

object GETConstants {
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
sealed class GETConfiguration {
    object Disabled : GETConfiguration() {
        override fun useGET(buffer: Buffer): Boolean {
            return false
        }
    }

    class EnabledWithFallback(val maxMessageBytes: Int = 50_000) : GETConfiguration() {
        override fun useGET(buffer: Buffer): Boolean {
            return maxMessageBytes > buffer.size
        }
    }

    object Enabled : GETConfiguration() {
        override fun useGET(buffer: Buffer): Boolean {
            return true
        }
    }

    abstract fun useGET(buffer: Buffer): Boolean
}

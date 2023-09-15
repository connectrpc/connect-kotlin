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

package com.connectrpc

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.Test
import org.mockito.kotlin.mock
import java.net.MalformedURLException


class ProtocolClientConfigTest {

    @Test
    fun hostUri() {
        val config = ProtocolClientConfig(
            host = "https://connectrpc.com",
            serializationStrategy = mock { }
        )
        assertThat(config.baseUri.host).isEqualTo("connectrpc.com")
        assertThat(config.baseUri.toURL()).isNotNull()
    }

    @Test(expected = MalformedURLException::class)
    fun unsupportedSchemeErrorsWhenTranslatingToURL() {
        val config = ProtocolClientConfig(
            host = "xhtp://connectrpc.com",
            serializationStrategy = mock { }
        )
        config.baseUri.toURL()
        fail<Unit>("expecting URL construction to fail")
    }
}

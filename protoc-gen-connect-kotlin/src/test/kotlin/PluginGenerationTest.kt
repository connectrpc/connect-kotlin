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

import buf.javamultiplefiles.disabled.v1.DisabledServiceClient
import buf.javamultiplefiles.enabled.v1.EnabledServiceClient
import buf.javamultiplefiles.unspecified.v1.UnspecifiedServiceClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class PluginGenerationTest {

    @Test
    fun emptyPackagePresence() {
        val request = NoPackage.SayRequest.newBuilder().setSentence("hello").build()
        assertThat(request.sentence).isEqualTo("hello")
    }

    @Test
    fun multiFileEnabled() {
        assertThat(EnabledServiceClient::class.java).isNotNull
    }

    @Test
    fun multiFileDisabled() {
        assertThat(DisabledServiceClient::class.java).isNotNull
    }

    @Test
    fun multiFileUnspecified() {
        assertThat(UnspecifiedServiceClient::class.java).isNotNull
    }
}

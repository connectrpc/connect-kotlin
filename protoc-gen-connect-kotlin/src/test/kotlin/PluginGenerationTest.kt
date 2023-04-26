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

import buf.evilcomments.v1.EvilCommentsServiceClient
import buf.javamultiplefiles.disabled.v1.DisabledEmptyOuterClass
import buf.javamultiplefiles.disabled.v1.DisabledEmptyServiceClient
import buf.javamultiplefiles.disabled.v1.DisabledInnerMessageServiceClient
import buf.javamultiplefiles.disabled.v1.DisabledServiceClient
import buf.javamultiplefiles.enabled.v1.EnabledEmptyRPCRequest
import buf.javamultiplefiles.enabled.v1.EnabledEmptyRPCResponse
import buf.javamultiplefiles.enabled.v1.EnabledEmptyServiceClient
import buf.javamultiplefiles.enabled.v1.EnabledInnerMessageServiceClient
import buf.javamultiplefiles.enabled.v1.EnabledServiceClient
import buf.javamultiplefiles.unspecified.v1.UnspecifiedEmptyOuterClass
import buf.javamultiplefiles.unspecified.v1.UnspecifiedEmptyServiceClient
import buf.javamultiplefiles.unspecified.v1.UnspecifiedInnerMessageServiceClient
import buf.javamultiplefiles.unspecified.v1.UnspecifiedServiceClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock

class PluginGenerationTest {

    @Test
    fun emptyPackagePresence() {
        val request = NoPackage.SayRequest.newBuilder().setSentence("hello").build()
        assertThat(request.sentence).isEqualTo("hello")
    }

    @Test
    fun emptyPackageServiceRequest() {
        val client = ElizaServiceClient(mock { })
        val request = NoPackage.SayRequest.newBuilder().setSentence("hello").build()
        CoroutineScope(Dispatchers.IO).launch {
            client.say(request, emptyMap())
        }
        assertThat(request.sentence).isEqualTo("hello")
    }

    @Test
    fun multiFileEnabled() {
        val client = EnabledEmptyServiceClient(mock { })
        CoroutineScope(Dispatchers.IO).launch {
            client.enabledEmptyRPC(EnabledEmptyRPCRequest.getDefaultInstance())
        }
        assertThat(EnabledEmptyServiceClient::class.java).isNotNull
        assertThat(EnabledServiceClient::class.java).isNotNull
        assertThat(EnabledInnerMessageServiceClient::class.java).isNotNull
    }

    @Test
    fun multiFileDisabled() {
        val client = DisabledEmptyServiceClient(mock { })
        CoroutineScope(Dispatchers.IO).launch {
            client.disabledEmptyRPC(DisabledEmptyOuterClass.DisabledEmptyRPCRequest.getDefaultInstance())
        }
        assertThat(DisabledEmptyServiceClient::class.java).isNotNull
        assertThat(DisabledServiceClient::class.java).isNotNull
        assertThat(DisabledInnerMessageServiceClient::class.java).isNotNull
    }

    @Test
    fun multiFileUnspecified() {
        val client = UnspecifiedEmptyServiceClient(mock { })
        CoroutineScope(Dispatchers.IO).launch {
            client.unspecifiedEmptyRPC(UnspecifiedEmptyOuterClass.UnspecifiedEmptyRPCRequest.getDefaultInstance())
        }
        assertThat(UnspecifiedEmptyServiceClient::class.java).isNotNull
        assertThat(UnspecifiedServiceClient::class.java).isNotNull
        assertThat(UnspecifiedInnerMessageServiceClient::class.java).isNotNull
    }

    @Test
    fun callbackSignature() {
        val unspecifiedEmptyServiceClient = UnspecifiedEmptyServiceClient(mock { })
        val request = UnspecifiedEmptyOuterClass.UnspecifiedEmptyRPCRequest.getDefaultInstance()
        unspecifiedEmptyServiceClient.unspecifiedEmptyRPC(request) { response ->
            response.success { success ->
                assertThat(success.message).isOfAnyClassIn(UnspecifiedEmptyOuterClass.UnspecifiedEmptyRPCResponse::class.java)
            }
        }
        val disabledEmptyServiceClient = DisabledEmptyServiceClient(mock { })
        disabledEmptyServiceClient.disabledEmptyRPC(DisabledEmptyOuterClass.DisabledEmptyRPCRequest.getDefaultInstance()) { response ->
            response.success { success ->
                success.message
                assertThat(success.message).isOfAnyClassIn(DisabledEmptyOuterClass.DisabledEmptyRPCResponse::class.java)
            }
        }
        val enabledEmptyServiceClient = EnabledEmptyServiceClient(mock { })
        enabledEmptyServiceClient.enabledEmptyRPC(EnabledEmptyRPCRequest.getDefaultInstance()) { response ->
            response.success { success ->
                success.message
                assertThat(success.message).isOfAnyClassIn(EnabledEmptyRPCResponse::class.java)
            }
        }
    }

    @Test
    fun evilCommentsCompiles() {
        val client = EvilCommentsServiceClient(mock { })
    }
}

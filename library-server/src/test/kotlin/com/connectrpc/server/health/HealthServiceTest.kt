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

package com.connectrpc.server.health

import com.connectrpc.StreamType
import com.connectrpc.server.ServerContextImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class HealthServiceTest {

    @Test
    fun `check returns SERVING for overall server health by default`() = runTest {
        val service = HealthService()
        val ctx = createContext()

        val response = service.check(ctx, HealthCheckRequest(""))

        assertThat(response.status).isEqualTo(ServingStatus.SERVING)
    }

    @Test
    fun `check returns SERVICE_UNKNOWN for unregistered service`() = runTest {
        val service = HealthService()
        val ctx = createContext()

        val response = service.check(ctx, HealthCheckRequest("unknown.Service"))

        assertThat(response.status).isEqualTo(ServingStatus.SERVICE_UNKNOWN)
    }

    @Test
    fun `check returns set status for registered service`() = runTest {
        val service = HealthService()
        service.setStatus("my.Service", ServingStatus.SERVING)
        val ctx = createContext()

        val response = service.check(ctx, HealthCheckRequest("my.Service"))

        assertThat(response.status).isEqualTo(ServingStatus.SERVING)
    }

    @Test
    fun `setStatus updates service status`() = runTest {
        val service = HealthService()
        val ctx = createContext()

        service.setStatus("my.Service", ServingStatus.SERVING)
        assertThat(service.check(ctx, HealthCheckRequest("my.Service")).status)
            .isEqualTo(ServingStatus.SERVING)

        service.setStatus("my.Service", ServingStatus.NOT_SERVING)
        assertThat(service.check(ctx, HealthCheckRequest("my.Service")).status)
            .isEqualTo(ServingStatus.NOT_SERVING)
    }

    @Test
    fun `clearStatus removes service status`() = runTest {
        val service = HealthService()
        val ctx = createContext()

        service.setStatus("my.Service", ServingStatus.SERVING)
        service.clearStatus("my.Service")

        val response = service.check(ctx, HealthCheckRequest("my.Service"))
        assertThat(response.status).isEqualTo(ServingStatus.SERVICE_UNKNOWN)
    }

    @Test
    fun `clearAll removes all service statuses`() = runTest {
        val service = HealthService()
        val ctx = createContext()

        service.setStatus("service1", ServingStatus.SERVING)
        service.setStatus("service2", ServingStatus.SERVING)
        service.clearAll()

        assertThat(service.check(ctx, HealthCheckRequest("service1")).status)
            .isEqualTo(ServingStatus.SERVICE_UNKNOWN)
        assertThat(service.check(ctx, HealthCheckRequest("service2")).status)
            .isEqualTo(ServingStatus.SERVICE_UNKNOWN)
    }

    @Test
    fun `serverStatus affects overall health check`() = runTest {
        val service = HealthService()
        val ctx = createContext()

        service.serverStatus = ServingStatus.NOT_SERVING

        val response = service.check(ctx, HealthCheckRequest(""))
        assertThat(response.status).isEqualTo(ServingStatus.NOT_SERVING)
    }

    @Test
    fun `getAllStatuses returns all registered statuses`() {
        val service = HealthService()

        service.setStatus("service1", ServingStatus.SERVING)
        service.setStatus("service2", ServingStatus.NOT_SERVING)

        val statuses = service.getAllStatuses()
        assertThat(statuses).containsEntry("service1", ServingStatus.SERVING)
        assertThat(statuses).containsEntry("service2", ServingStatus.NOT_SERVING)
    }

    @Test
    fun `asHandlerSpecs returns Check handler`() {
        val service = HealthService()

        val specs = service.asHandlerSpecs()

        assertThat(specs).hasSize(1)
        assertThat(specs[0].procedure).isEqualTo("grpc.health.v1.Health/Check")
        assertThat(specs[0].streamType).isEqualTo(StreamType.UNARY)
    }

    @Test
    fun `serviceName is correct`() {
        val service = HealthService()

        assertThat(service.serviceName).isEqualTo("grpc.health.v1.Health")
    }

    private fun createContext(): ServerContextImpl {
        return ServerContextImpl(
            requestHeaders = emptyMap(),
            procedure = "grpc.health.v1.Health/Check",
            timeout = null,
            coroutineContext = Dispatchers.Default,
        )
    }
}

class HealthCheckCodecTest {

    @Test
    fun `HealthCheckRequestCodec serializes empty service`() {
        val codec = HealthCheckRequestCodec()
        val request = HealthCheckRequest("")

        val buffer = codec.serialize(request)

        assertThat(buffer.readUtf8()).isEqualTo("{}")
    }

    @Test
    fun `HealthCheckRequestCodec serializes service name`() {
        val codec = HealthCheckRequestCodec()
        val request = HealthCheckRequest("my.Service")

        val buffer = codec.serialize(request)

        assertThat(buffer.readUtf8()).isEqualTo("""{"service":"my.Service"}""")
    }

    @Test
    fun `HealthCheckRequestCodec deserializes empty object`() {
        val codec = HealthCheckRequestCodec()

        val request = codec.deserialize(okio.Buffer().writeUtf8("{}"))

        assertThat(request.service).isEmpty()
    }

    @Test
    fun `HealthCheckRequestCodec deserializes service name`() {
        val codec = HealthCheckRequestCodec()

        val request = codec.deserialize(okio.Buffer().writeUtf8("""{"service":"my.Service"}"""))

        assertThat(request.service).isEqualTo("my.Service")
    }

    @Test
    fun `HealthCheckResponseCodec serializes status`() {
        val codec = HealthCheckResponseCodec()
        val response = HealthCheckResponse(ServingStatus.SERVING)

        val buffer = codec.serialize(response)

        assertThat(buffer.readUtf8()).isEqualTo("""{"status":"SERVING"}""")
    }

    @Test
    fun `HealthCheckResponseCodec deserializes status`() {
        val codec = HealthCheckResponseCodec()

        val response = codec.deserialize(okio.Buffer().writeUtf8("""{"status":"NOT_SERVING"}"""))

        assertThat(response.status).isEqualTo(ServingStatus.NOT_SERVING)
    }
}

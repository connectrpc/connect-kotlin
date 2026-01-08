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

import com.connectrpc.AnyError
import com.connectrpc.Codec
import com.connectrpc.ConnectErrorDetail
import com.connectrpc.ErrorDetailParser
import com.connectrpc.SerializationStrategy
import com.connectrpc.StreamType
import com.connectrpc.server.HandlerSpec
import com.connectrpc.server.ServerContext
import com.connectrpc.server.ServiceHandler
import com.connectrpc.server.UnaryHandler
import okio.Buffer
import okio.BufferedSource
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

/**
 * gRPC Health Checking service implementation.
 *
 * Implements the standard gRPC health checking protocol.
 * https://github.com/grpc/grpc/blob/master/doc/health-checking.md
 *
 * Usage:
 * ```kotlin
 * val healthService = HealthService()
 * server.registerHandlers(*healthService.asHandlerSpecs().toTypedArray())
 *
 * // Set service status
 * healthService.setStatus("my.Service", ServingStatus.SERVING)
 * ```
 */
class HealthService : ServiceHandler {
    override val serviceName: String = "grpc.health.v1.Health"

    private val statuses = ConcurrentHashMap<String, ServingStatus>()

    /**
     * The overall server status. Defaults to SERVING.
     */
    var serverStatus: ServingStatus = ServingStatus.SERVING

    /**
     * Checks the health of a service.
     *
     * @param ctx The server context.
     * @param request The health check request.
     * @return The health check response.
     */
    suspend fun check(ctx: ServerContext, request: HealthCheckRequest): HealthCheckResponse {
        val status = if (request.service.isEmpty()) {
            // Empty service name means overall server health
            serverStatus
        } else {
            statuses[request.service] ?: ServingStatus.SERVICE_UNKNOWN
        }
        return HealthCheckResponse(status)
    }

    /**
     * Sets the serving status for a specific service.
     *
     * @param service The service name.
     * @param status The serving status.
     */
    fun setStatus(service: String, status: ServingStatus) {
        statuses[service] = status
    }

    /**
     * Clears the serving status for a specific service.
     *
     * @param service The service name.
     */
    fun clearStatus(service: String) {
        statuses.remove(service)
    }

    /**
     * Clears all service statuses.
     */
    fun clearAll() {
        statuses.clear()
    }

    /**
     * Gets all registered service statuses.
     */
    fun getAllStatuses(): Map<String, ServingStatus> {
        return statuses.toMap()
    }

    /**
     * Returns handler specifications for this service.
     */
    fun asHandlerSpecs(): List<HandlerSpec<*, *>> {
        return listOf(
            HandlerSpec(
                procedure = "$serviceName/Check",
                requestClass = HealthCheckRequest::class,
                responseClass = HealthCheckResponse::class,
                streamType = StreamType.UNARY,
                handler = UnaryHandler { ctx, req -> check(ctx, req) },
            ),
        )
    }
}

/**
 * Serialization strategy for Health Check service.
 *
 * Provides JSON serialization for HealthCheckRequest/Response.
 */
class HealthSerializationStrategy : SerializationStrategy {
    override fun serializationName(): String = "json"

    @Suppress("UNCHECKED_CAST")
    override fun <E : Any> codec(clazz: KClass<E>): Codec<E> {
        return when (clazz) {
            HealthCheckRequest::class -> HealthCheckRequestCodec() as Codec<E>
            HealthCheckResponse::class -> HealthCheckResponseCodec() as Codec<E>
            else -> throw IllegalArgumentException("Unsupported type: $clazz")
        }
    }

    override fun errorDetailParser(): ErrorDetailParser {
        return object : ErrorDetailParser {
            override fun <E : Any> unpack(any: AnyError, clazz: KClass<E>): E? = null
            override fun parseDetails(bytes: ByteArray): List<ConnectErrorDetail> = emptyList()
        }
    }
}

/**
 * JSON codec for HealthCheckRequest.
 */
internal class HealthCheckRequestCodec : Codec<HealthCheckRequest> {
    override fun encodingName(): String = "json"

    override fun serialize(message: HealthCheckRequest): Buffer {
        val json = if (message.service.isEmpty()) {
            "{}"
        } else {
            """{"service":"${escapeJson(message.service)}"}"""
        }
        return Buffer().writeUtf8(json)
    }

    override fun deterministicSerialize(message: HealthCheckRequest): Buffer = serialize(message)

    override fun deserialize(source: BufferedSource): HealthCheckRequest {
        val json = source.readUtf8()
        // Simple JSON parsing for {"service": "value"}
        val serviceMatch = Regex(""""service"\s*:\s*"([^"]*)"""").find(json)
        val service = serviceMatch?.groupValues?.get(1) ?: ""
        return HealthCheckRequest(service)
    }

    private fun escapeJson(s: String): String {
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
    }
}

/**
 * JSON codec for HealthCheckResponse.
 */
internal class HealthCheckResponseCodec : Codec<HealthCheckResponse> {
    override fun encodingName(): String = "json"

    override fun serialize(message: HealthCheckResponse): Buffer {
        val json = """{"status":"${message.status.name}"}"""
        return Buffer().writeUtf8(json)
    }

    override fun deterministicSerialize(message: HealthCheckResponse): Buffer = serialize(message)

    override fun deserialize(source: BufferedSource): HealthCheckResponse {
        val json = source.readUtf8()
        // Simple JSON parsing for {"status": "SERVING"}
        val statusMatch = Regex(""""status"\s*:\s*"([^"]*)"""").find(json)
        val statusName = statusMatch?.groupValues?.get(1) ?: "UNKNOWN"
        val status = try {
            ServingStatus.valueOf(statusName)
        } catch (e: IllegalArgumentException) {
            ServingStatus.UNKNOWN
        }
        return HealthCheckResponse(status)
    }
}

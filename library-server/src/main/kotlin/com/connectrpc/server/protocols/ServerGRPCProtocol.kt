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

package com.connectrpc.server.protocols

import com.connectrpc.Code
import com.connectrpc.ConnectException
import com.connectrpc.Headers
import com.connectrpc.compression.CompressionPool
import com.connectrpc.protocols.Envelope
import com.connectrpc.server.ServerConfig
import com.connectrpc.server.http.HTTPServerCall
import com.connectrpc.server.http.ServerRequest
import com.connectrpc.server.http.ServerResponse
import okio.Buffer
import okio.ByteString.Companion.encodeUtf8
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

/**
 * Server-side gRPC protocol handler.
 *
 * Implements the gRPC HTTP/2 protocol.
 * https://github.com/grpc/grpc/blob/master/doc/PROTOCOL-HTTP2.md
 */
class ServerGRPCProtocol(
    private val config: ServerConfig,
) {
    companion object {
        private const val CONTENT_TYPE = "content-type"
        private const val GRPC_ENCODING = "grpc-encoding"
        private const val GRPC_ACCEPT_ENCODING = "grpc-accept-encoding"
        private const val GRPC_TIMEOUT = "grpc-timeout"
        private const val GRPC_STATUS = "grpc-status"
        private const val GRPC_MESSAGE = "grpc-message"
        private const val GRPC_STATUS_DETAILS_BIN = "grpc-status-details-bin"
    }

    /**
     * Parses an incoming gRPC HTTP call into a ServerRequest.
     *
     * @param call The HTTP server call.
     * @return The parsed server request.
     * @throws ConnectException If the request is invalid.
     */
    suspend fun parseRequest(call: HTTPServerCall): ServerRequest {
        // Parse procedure from path
        val path = call.path.removePrefix("/")
        val (serviceName, methodName) = parseProcedure(path)

        // Parse content type
        val contentType = call.contentType ?: throw ConnectException(
            code = Code.INVALID_ARGUMENT,
            message = "missing content-type header",
        )

        // Validate content type
        if (!isGRPCContentType(contentType)) {
            throw ConnectException(
                code = Code.INVALID_ARGUMENT,
                message = "invalid content-type for gRPC: $contentType",
            )
        }

        // Parse timeout from grpc-timeout header
        val timeout = parseGRPCTimeout(call.requestHeaders)

        // Parse compression
        val contentEncoding = call.requestHeaders[GRPC_ENCODING]?.firstOrNull()
        val compressionPool = if (contentEncoding != null) {
            config.compressionPool(contentEncoding) ?: throw ConnectException(
                code = Code.UNIMPLEMENTED,
                message = "unsupported compression: $contentEncoding",
            )
        } else {
            null
        }

        // Read body and unpack from envelope
        val rawBody = call.receiveBody()
        val (_, message) = Envelope.unpackWithHeaderByte(rawBody, compressionPool)

        return ServerRequest(
            serviceName = serviceName,
            methodName = methodName,
            procedure = path,
            headers = call.requestHeaders,
            message = message,
            timeout = timeout,
            contentType = contentType,
            isCompressed = compressionPool != null,
            compression = contentEncoding,
        )
    }

    /**
     * Sends a gRPC response.
     *
     * @param call The HTTP server call.
     * @param response The server response.
     * @param acceptEncoding The compression methods accepted by the client.
     */
    suspend fun sendResponse(
        call: HTTPServerCall,
        response: ServerResponse,
        acceptEncoding: List<String>,
    ) {
        when (response) {
            is ServerResponse.Success -> sendSuccessResponse(call, response, acceptEncoding)
            is ServerResponse.Failure -> sendErrorResponse(call, response)
        }
    }

    private suspend fun sendSuccessResponse(
        call: HTTPServerCall,
        response: ServerResponse.Success,
        acceptEncoding: List<String>,
    ) {
        val headers = mutableMapOf<String, List<String>>()
        headers.putAll(response.headers)

        // Set content type
        headers[CONTENT_TYPE] = listOf("application/grpc+${config.serializationStrategy.serializationName()}")

        // Determine response compression
        val compressionPool = selectResponseCompression(acceptEncoding, response.message.size)
        if (compressionPool != null) {
            headers[GRPC_ENCODING] = listOf(compressionPool.name())
        }

        // Pack response into envelope
        val envelopedMessage = Envelope.pack(
            response.message,
            compressionPool,
            config.compressionMinBytes,
        )

        call.respondHeaders(200, headers)
        call.respondBody(envelopedMessage)

        // Send trailers with grpc-status
        val trailers = response.trailers.toMutableMap()
        trailers[GRPC_STATUS] = listOf("0") // OK
        call.respondTrailers(trailers)
    }

    private suspend fun sendErrorResponse(
        call: HTTPServerCall,
        response: ServerResponse.Failure,
    ) {
        val error = response.error
        val headers = mutableMapOf<String, List<String>>()
        headers.putAll(response.headers)
        headers[CONTENT_TYPE] = listOf("application/grpc+${config.serializationStrategy.serializationName()}")

        call.respondHeaders(200, headers) // gRPC always uses 200 OK
        call.respondBody(Buffer()) // Empty body for error responses

        // Send trailers with error information
        val trailers = response.trailers.toMutableMap()
        trailers[GRPC_STATUS] = listOf(error.code.value.toString())
        error.message?.let { message ->
            trailers[GRPC_MESSAGE] = listOf(grpcPercentEncode(message))
        }

        // Encode full status proto (includes code, message, and details)
        val statusProto = encodeStatusProto(error)
        if (statusProto != null) {
            trailers[GRPC_STATUS_DETAILS_BIN] = listOf(statusProto)
        }

        call.respondTrailers(trailers)
    }

    private fun selectResponseCompression(
        acceptEncoding: List<String>,
        responseSize: Long,
    ): CompressionPool? {
        if (!config.compressResponses) return null
        if (responseSize < config.compressionMinBytes) return null

        val preferred = config.preferredCompression ?: return null
        return if (acceptEncoding.contains(preferred.name())) {
            preferred
        } else {
            null
        }
    }

    private fun parseProcedure(path: String): Pair<String, String> {
        val lastSlash = path.lastIndexOf('/')
        if (lastSlash < 0) {
            throw ConnectException(
                code = Code.INVALID_ARGUMENT,
                message = "invalid procedure path: $path",
            )
        }
        return Pair(path.substring(0, lastSlash), path.substring(lastSlash + 1))
    }

    /**
     * Parses grpc-timeout header.
     * Format: 1-8 digit integer + unit (H=hours, M=minutes, S=seconds, m=millis, u=micros, n=nanos)
     */
    private fun parseGRPCTimeout(headers: Headers): Duration? {
        val timeoutStr = headers[GRPC_TIMEOUT]?.firstOrNull() ?: return null
        if (timeoutStr.isEmpty()) return null

        val unit = timeoutStr.last()
        val valueStr = timeoutStr.dropLast(1)
        val value = valueStr.toLongOrNull() ?: return null

        return when (unit) {
            'H' -> value.toDuration(DurationUnit.HOURS)
            'M' -> value.toDuration(DurationUnit.MINUTES)
            'S' -> value.toDuration(DurationUnit.SECONDS)
            'm' -> value.toDuration(DurationUnit.MILLISECONDS)
            'u' -> value.toDuration(DurationUnit.MICROSECONDS)
            'n' -> value.toDuration(DurationUnit.NANOSECONDS)
            else -> null
        }
    }

    /**
     * Percent-encodes a string for use in grpc-message header.
     * https://github.com/grpc/grpc/blob/master/doc/PROTOCOL-HTTP2.md#responses
     */
    private fun grpcPercentEncode(message: String): String {
        val bytes = message.encodeUtf8()
        val sb = StringBuilder()
        for (i in 0 until bytes.size) {
            val b = bytes[i].toInt() and 0xFF
            if (b in 0x20..0x7E && b != '%'.code) {
                sb.append(b.toChar())
            } else {
                sb.append('%')
                sb.append(String.format("%02X", b))
            }
        }
        return sb.toString()
    }

    /**
     * Encodes error as google.rpc.Status proto and returns base64-encoded string.
     * Returns null if encoding fails.
     */
    private fun encodeStatusProto(error: ConnectException): String? {
        return try {
            val statusBytes = StatusProtoEncoder.encode(
                code = error.code,
                message = error.message,
                details = error.details,
            )
            okio.ByteString.of(*statusBytes).base64()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Gets the client's accepted compression encodings.
     */
    fun getAcceptEncoding(headers: Headers): List<String> {
        return headers[GRPC_ACCEPT_ENCODING]?.flatMap { it.split(",").map(String::trim) } ?: emptyList()
    }

    /**
     * Checks if this call should be handled by the gRPC protocol.
     */
    fun canHandle(call: HTTPServerCall): Boolean {
        if (call.method != "POST") return false
        val contentType = call.contentType ?: return false
        return isGRPCContentType(contentType)
    }

    private fun isGRPCContentType(contentType: String): Boolean {
        return contentType == "application/grpc" ||
            contentType.startsWith("application/grpc+")
    }
}

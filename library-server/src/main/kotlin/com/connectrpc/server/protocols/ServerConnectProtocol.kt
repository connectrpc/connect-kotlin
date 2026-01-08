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
import com.connectrpc.ConnectErrorDetail
import com.connectrpc.ConnectException
import com.connectrpc.Headers
import com.connectrpc.compression.CompressionPool
import com.connectrpc.server.ServerConfig
import com.connectrpc.server.http.HTTPServerCall
import com.connectrpc.server.http.ServerRequest
import com.connectrpc.server.http.ServerResponse
import okio.Buffer
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

/**
 * Server-side Connect protocol handler.
 *
 * Handles the Connect protocol for both unary and streaming RPCs.
 * https://connectrpc.com/docs/protocol
 */
class ServerConnectProtocol(
    private val config: ServerConfig,
) {
    companion object {
        private const val CONNECT_PROTOCOL_VERSION_KEY = "connect-protocol-version"
        private const val CONNECT_PROTOCOL_VERSION_VALUE = "1"
        private const val CONNECT_TIMEOUT_MS = "connect-timeout-ms"
        private const val CONTENT_TYPE = "content-type"
        private const val CONTENT_ENCODING = "content-encoding"
        private const val ACCEPT_ENCODING = "accept-encoding"
        private const val CONNECT_ACCEPT_ENCODING = "connect-accept-encoding"
        private const val CONNECT_CONTENT_ENCODING = "connect-content-encoding"
    }

    /**
     * Parses an incoming HTTP call into a ServerRequest.
     *
     * @param call The HTTP server call.
     * @return The parsed server request.
     * @throws ConnectException If the request is invalid.
     */
    suspend fun parseRequest(call: HTTPServerCall): ServerRequest {
        // Validate Connect protocol version
        val protocolVersion = call.requestHeaders[CONNECT_PROTOCOL_VERSION_KEY]?.firstOrNull()
        if (protocolVersion != null && protocolVersion != CONNECT_PROTOCOL_VERSION_VALUE) {
            throw ConnectException(
                code = Code.INVALID_ARGUMENT,
                message = "unsupported connect protocol version: $protocolVersion",
            )
        }

        // Parse procedure from path
        val path = call.path.removePrefix("/")
        val (serviceName, methodName) = parseProcedure(path)

        // Parse content type
        val contentType = call.contentType ?: throw ConnectException(
            code = Code.INVALID_ARGUMENT,
            message = "missing content-type header",
        )

        // Parse timeout
        val timeout = parseTimeout(call.requestHeaders)

        // Parse compression
        val contentEncoding = call.requestHeaders[CONTENT_ENCODING]?.firstOrNull()
        val compressionPool = if (contentEncoding != null) {
            config.compressionPool(contentEncoding) ?: throw ConnectException(
                code = Code.UNIMPLEMENTED,
                message = "unsupported compression: $contentEncoding",
            )
        } else {
            null
        }

        // Read and decompress body
        val rawBody = call.receiveBody()
        val body = if (compressionPool != null) {
            compressionPool.decompress(rawBody)
        } else {
            rawBody
        }

        return ServerRequest(
            serviceName = serviceName,
            methodName = methodName,
            procedure = path,
            headers = call.requestHeaders,
            message = body,
            timeout = timeout,
            contentType = contentType,
            isCompressed = compressionPool != null,
            compression = contentEncoding,
        )
    }

    /**
     * Formats a server response for sending over HTTP.
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

        // Determine response compression
        val compressionPool = selectResponseCompression(acceptEncoding, response.message.size)
        val responseBody = if (compressionPool != null) {
            headers[CONTENT_ENCODING] = listOf(compressionPool.name())
            compressionPool.compress(response.message)
        } else {
            response.message
        }

        // Set content type based on request
        if (!headers.containsKey(CONTENT_TYPE)) {
            headers[CONTENT_TYPE] = listOf(config.serializationStrategy.serializationName().let {
                "application/$it"
            })
        }

        call.respondHeaders(200, headers)
        call.respondBody(responseBody)
        call.respondTrailers(response.trailers)
    }

    private suspend fun sendErrorResponse(
        call: HTTPServerCall,
        response: ServerResponse.Failure,
    ) {
        val error = response.error
        val httpStatus = errorCodeToHTTPStatus(error.code)

        val headers = mutableMapOf<String, List<String>>()
        headers.putAll(response.headers)
        headers[CONTENT_TYPE] = listOf("application/json")

        val errorJson = buildErrorJson(error)

        call.respondHeaders(httpStatus, headers)
        call.respondBody(Buffer().writeUtf8(errorJson))
        call.respondTrailers(response.trailers)
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

    private fun parseTimeout(headers: Headers): Duration? {
        val timeoutMs = headers[CONNECT_TIMEOUT_MS]?.firstOrNull() ?: return null
        return try {
            timeoutMs.toLong().toDuration(DurationUnit.MILLISECONDS)
        } catch (e: NumberFormatException) {
            null
        }
    }

    private fun errorCodeToHTTPStatus(code: Code): Int {
        return when (code) {
            Code.CANCELED -> 408
            Code.UNKNOWN -> 500
            Code.INVALID_ARGUMENT -> 400
            Code.DEADLINE_EXCEEDED -> 408
            Code.NOT_FOUND -> 404
            Code.ALREADY_EXISTS -> 409
            Code.PERMISSION_DENIED -> 403
            Code.RESOURCE_EXHAUSTED -> 429
            Code.FAILED_PRECONDITION -> 400
            Code.ABORTED -> 409
            Code.OUT_OF_RANGE -> 400
            Code.UNIMPLEMENTED -> 404
            Code.INTERNAL_ERROR -> 500
            Code.UNAVAILABLE -> 503
            Code.DATA_LOSS -> 500
            Code.UNAUTHENTICATED -> 401
        }
    }

    private fun buildErrorJson(error: ConnectException): String {
        val sb = StringBuilder()
        sb.append("{")
        sb.append("\"code\":\"${error.code.codeName}\"")
        error.message?.let { message ->
            sb.append(",\"message\":\"${escapeJson(message)}\"")
        }
        if (error.details.isNotEmpty()) {
            sb.append(",\"details\":[")
            error.details.forEachIndexed { index, detail ->
                if (index > 0) sb.append(",")
                sb.append("{\"type\":\"${detail.type}\",\"value\":\"${detail.payload.base64()}\"}")
            }
            sb.append("]")
        }
        sb.append("}")
        return sb.toString()
    }

    private fun escapeJson(s: String): String {
        return s
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    /**
     * Gets the client's accepted compression encodings.
     */
    fun getAcceptEncoding(headers: Headers): List<String> {
        return headers[ACCEPT_ENCODING]?.flatMap { it.split(",").map(String::trim) } ?: emptyList()
    }

    /**
     * Checks if this call should be handled by the Connect protocol.
     */
    fun canHandle(call: HTTPServerCall): Boolean {
        // Connect protocol uses POST with application/json or application/proto content types
        // or GET for idempotent methods
        if (call.method != "POST" && call.method != "GET") {
            return false
        }

        val contentType = call.contentType ?: return call.method == "GET"

        // Connect unary: application/json, application/proto
        // Connect streaming: application/connect+json, application/connect+proto
        return contentType.startsWith("application/json") ||
            contentType.startsWith("application/proto") ||
            contentType.startsWith("application/connect+")
    }
}

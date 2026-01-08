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

package com.connectrpc.ktor

import com.connectrpc.Headers
import com.connectrpc.Trailers
import com.connectrpc.server.http.HTTPServerCall
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.contentType
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.response.header
import io.ktor.server.response.respondBytes
import io.ktor.utils.io.jvm.javaio.toInputStream
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okio.Buffer

/**
 * Ktor implementation of [HTTPServerCall].
 *
 * Adapts Ktor's [ApplicationCall] to the Connect server's HTTP abstraction.
 *
 * @param call The Ktor application call.
 * @param basePath Optional base path to strip from the request path.
 */
class KtorHTTPServerCall(
    private val call: ApplicationCall,
    private val basePath: String = "",
) : HTTPServerCall {

    override val method: String
        get() = call.request.httpMethod.value

    override val path: String
        get() {
            val fullPath = call.request.path()
            return if (basePath.isNotEmpty() && fullPath.startsWith(basePath)) {
                fullPath.removePrefix(basePath)
            } else {
                fullPath
            }
        }

    override val requestHeaders: Headers
        get() {
            val headers = mutableMapOf<String, List<String>>()
            call.request.headers.forEach { key, values ->
                headers[key.lowercase()] = values
            }
            return headers
        }

    override val contentType: String?
        get() = call.request.contentType().toString().takeIf { it.isNotBlank() && it != "*/*" }

    override suspend fun receiveBody(): Buffer {
        val buffer = Buffer()
        call.request.receiveChannel().toInputStream().use { input ->
            buffer.readFrom(input)
        }
        return buffer
    }

    override suspend fun respondHeaders(status: Int, headers: Headers) {
        // Set status code - will be sent with the response
        // Headers will be set before respondBytes
        headers.forEach { (key, values) ->
            values.forEach { value ->
                call.response.header(key, value)
            }
        }
        // Store status for later use
        responseStatus = status
    }

    private var responseStatus: Int = 200

    override suspend fun respondBody(body: Buffer) {
        call.respondBytes(
            bytes = body.readByteArray(),
            status = HttpStatusCode.fromValue(responseStatus),
        )
    }

    override suspend fun respondTrailers(trailers: Trailers) {
        // HTTP/1.1 doesn't support trailers directly
        // For Connect protocol over HTTP/1.1, trailers are encoded in the response body
        // For HTTP/2, Ktor handles trailers automatically
        // This is a simplified implementation - full trailer support requires more work
    }

    override suspend fun receiveStream(): Flow<Buffer> {
        // Streaming support - future enhancement
        return flow {
            emit(receiveBody())
        }
    }

    override suspend fun sendStream(messages: Flow<Buffer>) {
        // Streaming support - future enhancement
        throw UnsupportedOperationException("Streaming not yet implemented")
    }
}

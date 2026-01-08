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

package com.connectrpc.server

import com.connectrpc.AnyError
import com.connectrpc.Codec
import com.connectrpc.ConnectErrorDetail
import com.connectrpc.ErrorDetailParser
import com.connectrpc.Headers
import com.connectrpc.SerializationStrategy
import com.connectrpc.Trailers
import com.connectrpc.server.http.HTTPServerCall
import kotlinx.coroutines.flow.Flow
import okio.Buffer
import okio.BufferedSource
import kotlin.reflect.KClass

/**
 * Test serialization strategy that handles String types.
 */
class TestSerializationStrategy : SerializationStrategy {
    override fun serializationName(): String = "json"

    @Suppress("UNCHECKED_CAST")
    override fun <E : Any> codec(clazz: KClass<E>): Codec<E> {
        return TestStringCodec() as Codec<E>
    }

    override fun errorDetailParser(): ErrorDetailParser {
        return TestErrorDetailParser()
    }
}

/**
 * Simple codec that serializes strings as UTF-8.
 */
class TestStringCodec : Codec<String> {
    override fun encodingName(): String = "json"

    override fun serialize(message: String): Buffer {
        return Buffer().writeUtf8(message)
    }

    override fun deterministicSerialize(message: String): Buffer {
        return serialize(message)
    }

    override fun deserialize(source: BufferedSource): String {
        return source.readUtf8()
    }
}

/**
 * Test error detail parser that returns empty list.
 */
class TestErrorDetailParser : ErrorDetailParser {
    override fun <E : Any> unpack(any: AnyError, clazz: KClass<E>): E? = null
    override fun parseDetails(bytes: ByteArray): List<ConnectErrorDetail> = emptyList()
}

/**
 * Mock HTTP server call for testing.
 */
class MockHTTPServerCall(
    override val method: String = "POST",
    override val path: String = "/test.Service/Method",
    override val requestHeaders: Headers = emptyMap(),
    private val requestBody: Buffer = Buffer(),
) : HTTPServerCall {

    override val contentType: String?
        get() = requestHeaders["content-type"]?.firstOrNull()

    var respondedStatus: Int? = null
        private set
    var respondedHeaders: Headers? = null
        private set
    var respondedBody: Buffer? = null
        private set
    var respondedTrailers: Trailers? = null
        private set

    override suspend fun receiveBody(): Buffer {
        return requestBody
    }

    override suspend fun respondHeaders(status: Int, headers: Headers) {
        respondedStatus = status
        respondedHeaders = headers
    }

    override suspend fun respondBody(body: Buffer) {
        respondedBody = body
    }

    override suspend fun respondTrailers(trailers: Trailers) {
        respondedTrailers = trailers
    }

    override suspend fun receiveStream(): Flow<Buffer> {
        throw UnsupportedOperationException("receiveStream not supported in mock")
    }

    override suspend fun sendStream(messages: Flow<Buffer>) {
        throw UnsupportedOperationException("sendStream not supported in mock")
    }
}

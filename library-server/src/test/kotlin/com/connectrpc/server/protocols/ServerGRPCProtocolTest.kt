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

import com.connectrpc.AnyError
import com.connectrpc.Code
import com.connectrpc.ConnectErrorDetail
import com.connectrpc.ConnectException
import com.connectrpc.ErrorDetailParser
import com.connectrpc.protocols.Envelope
import okio.ByteString.Companion.toByteString
import com.connectrpc.server.MockHTTPServerCall
import kotlin.reflect.KClass
import com.connectrpc.server.ServerConfig
import com.connectrpc.server.TestSerializationStrategy
import com.connectrpc.server.http.ServerResponse
import kotlinx.coroutines.test.runTest
import okio.Buffer
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class ServerGRPCProtocolTest {

    private val config = ServerConfig(
        serializationStrategy = TestSerializationStrategy(),
    )
    private val protocol = ServerGRPCProtocol(config)

    @Test
    fun `canHandle returns true for gRPC content type`() {
        val call = MockHTTPServerCall(
            method = "POST",
            path = "/test.Service/Method",
            requestHeaders = mapOf("content-type" to listOf("application/grpc")),
        )
        assertThat(protocol.canHandle(call)).isTrue()
    }

    @Test
    fun `canHandle returns true for gRPC+proto content type`() {
        val call = MockHTTPServerCall(
            method = "POST",
            path = "/test.Service/Method",
            requestHeaders = mapOf("content-type" to listOf("application/grpc+proto")),
        )
        assertThat(protocol.canHandle(call)).isTrue()
    }

    @Test
    fun `canHandle returns true for gRPC+json content type`() {
        val call = MockHTTPServerCall(
            method = "POST",
            path = "/test.Service/Method",
            requestHeaders = mapOf("content-type" to listOf("application/grpc+json")),
        )
        assertThat(protocol.canHandle(call)).isTrue()
    }

    @Test
    fun `canHandle returns false for Connect content type`() {
        val call = MockHTTPServerCall(
            method = "POST",
            path = "/test.Service/Method",
            requestHeaders = mapOf("content-type" to listOf("application/json")),
        )
        assertThat(protocol.canHandle(call)).isFalse()
    }

    @Test
    fun `canHandle returns false for GET requests`() {
        val call = MockHTTPServerCall(
            method = "GET",
            path = "/test.Service/Method",
            requestHeaders = mapOf("content-type" to listOf("application/grpc")),
        )
        assertThat(protocol.canHandle(call)).isFalse()
    }

    @Test
    fun `parseRequest extracts procedure from path`() = runTest {
        val message = "test message"
        val envelopedBody = Envelope.pack(Buffer().writeUtf8(message))

        val call = MockHTTPServerCall(
            method = "POST",
            path = "/connectrpc.eliza.v1.ElizaService/Say",
            requestHeaders = mapOf("content-type" to listOf("application/grpc")),
            requestBody = envelopedBody,
        )

        val request = protocol.parseRequest(call)

        assertThat(request.serviceName).isEqualTo("connectrpc.eliza.v1.ElizaService")
        assertThat(request.methodName).isEqualTo("Say")
        assertThat(request.procedure).isEqualTo("connectrpc.eliza.v1.ElizaService/Say")
    }

    @Test
    fun `parseRequest unpacks enveloped message`() = runTest {
        val message = "hello world"
        val envelopedBody = Envelope.pack(Buffer().writeUtf8(message))

        val call = MockHTTPServerCall(
            method = "POST",
            path = "/test.Service/Method",
            requestHeaders = mapOf("content-type" to listOf("application/grpc")),
            requestBody = envelopedBody,
        )

        val request = protocol.parseRequest(call)

        assertThat(request.message.readUtf8()).isEqualTo(message)
    }

    @Test
    fun `parseRequest parses grpc-timeout in seconds`() = runTest {
        val envelopedBody = Envelope.pack(Buffer().writeUtf8("test"))

        val call = MockHTTPServerCall(
            method = "POST",
            path = "/test.Service/Method",
            requestHeaders = mapOf(
                "content-type" to listOf("application/grpc"),
                "grpc-timeout" to listOf("30S"),
            ),
            requestBody = envelopedBody,
        )

        val request = protocol.parseRequest(call)

        assertThat(request.timeout).isEqualTo(30.seconds)
    }

    @Test
    fun `parseRequest parses grpc-timeout in milliseconds`() = runTest {
        val envelopedBody = Envelope.pack(Buffer().writeUtf8("test"))

        val call = MockHTTPServerCall(
            method = "POST",
            path = "/test.Service/Method",
            requestHeaders = mapOf(
                "content-type" to listOf("application/grpc"),
                "grpc-timeout" to listOf("5000m"),
            ),
            requestBody = envelopedBody,
        )

        val request = protocol.parseRequest(call)

        assertThat(request.timeout).isEqualTo(5000.milliseconds)
    }

    @Test
    fun `sendResponse sends success with grpc-status 0`() = runTest {
        val call = MockHTTPServerCall(
            method = "POST",
            path = "/test.Service/Method",
            requestHeaders = mapOf("content-type" to listOf("application/grpc")),
        )

        val response = ServerResponse.Success(
            headers = emptyMap(),
            trailers = emptyMap(),
            message = Buffer().writeUtf8("response"),
        )

        protocol.sendResponse(call, response, emptyList())

        assertThat(call.respondedStatus).isEqualTo(200)
        assertThat(call.respondedTrailers?.get("grpc-status")).containsExactly("0")
    }

    @Test
    fun `sendResponse sends error with grpc-status and grpc-message`() = runTest {
        val call = MockHTTPServerCall(
            method = "POST",
            path = "/test.Service/Method",
            requestHeaders = mapOf("content-type" to listOf("application/grpc")),
        )

        val response = ServerResponse.Failure(
            error = ConnectException(
                code = Code.NOT_FOUND,
                message = "resource not found",
            ),
        )

        protocol.sendResponse(call, response, emptyList())

        assertThat(call.respondedStatus).isEqualTo(200) // gRPC always uses 200
        assertThat(call.respondedTrailers?.get("grpc-status")).containsExactly("5") // NOT_FOUND = 5
        assertThat(call.respondedTrailers?.get("grpc-message")).isNotNull()
    }

    @Test
    fun `sendResponse envelopes response message`() = runTest {
        val call = MockHTTPServerCall(
            method = "POST",
            path = "/test.Service/Method",
            requestHeaders = mapOf("content-type" to listOf("application/grpc")),
        )

        val response = ServerResponse.Success(
            headers = emptyMap(),
            trailers = emptyMap(),
            message = Buffer().writeUtf8("response data"),
        )

        protocol.sendResponse(call, response, emptyList())

        // Response should be enveloped (5 byte header + message)
        val responseBody = call.respondedBody!!
        assertThat(responseBody.size).isGreaterThan(5)

        // Unpack and verify
        val (_, unpackedMessage) = Envelope.unpackWithHeaderByte(responseBody)
        assertThat(unpackedMessage.readUtf8()).isEqualTo("response data")
    }

    @Test
    fun `getAcceptEncoding parses grpc-accept-encoding header`() {
        val headers = mapOf("grpc-accept-encoding" to listOf("gzip, identity"))
        val encodings = protocol.getAcceptEncoding(headers)

        assertThat(encodings).containsExactly("gzip", "identity")
    }

    @Test
    fun `sendResponse sends error with grpc-status-details-bin`() = runTest {
        val call = MockHTTPServerCall(
            method = "POST",
            path = "/test.Service/Method",
            requestHeaders = mapOf("content-type" to listOf("application/grpc")),
        )

        val errorDetail = ConnectErrorDetail(
            type = "type.googleapis.com/google.rpc.BadRequest",
            payload = "test detail payload".encodeToByteArray().toByteString(),
        )

        val errorParser = object : ErrorDetailParser {
            override fun <E : Any> unpack(any: AnyError, clazz: KClass<E>): E? = null
            override fun parseDetails(bytes: ByteArray): List<ConnectErrorDetail> = emptyList()
        }

        val response = ServerResponse.Failure(
            error = ConnectException(
                code = Code.INVALID_ARGUMENT,
                message = "invalid request",
            ).withErrorDetails(errorParser, listOf(errorDetail)),
        )

        protocol.sendResponse(call, response, emptyList())

        assertThat(call.respondedStatus).isEqualTo(200)
        assertThat(call.respondedTrailers?.get("grpc-status")).containsExactly("3") // INVALID_ARGUMENT = 3
        assertThat(call.respondedTrailers?.get("grpc-status-details-bin")).isNotNull()
        assertThat(call.respondedTrailers?.get("grpc-status-details-bin")?.first()).isNotEmpty()
    }

    @Test
    fun `sendResponse includes grpc-status-details-bin even without details`() = runTest {
        val call = MockHTTPServerCall(
            method = "POST",
            path = "/test.Service/Method",
            requestHeaders = mapOf("content-type" to listOf("application/grpc")),
        )

        val response = ServerResponse.Failure(
            error = ConnectException(
                code = Code.PERMISSION_DENIED,
                message = "access denied",
            ),
        )

        protocol.sendResponse(call, response, emptyList())

        // grpc-status-details-bin should be present (contains code and message)
        assertThat(call.respondedTrailers?.get("grpc-status-details-bin")).isNotNull()
    }
}

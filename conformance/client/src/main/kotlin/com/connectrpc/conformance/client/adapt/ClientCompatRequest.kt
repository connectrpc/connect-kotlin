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

package com.connectrpc.conformance.client.adapt

import com.connectrpc.Headers
import com.connectrpc.protocols.NetworkProtocol
import com.google.protobuf.ByteString

/**
 * ClientCompatRequest represents a request to the conformance client.
 * It describes the properties of an RPC that the client should issue.
 * This corresponds to the connectrpc.conformance.v1.ClientCompatRequest
 * proto message.
 *
 * We manually define this interface and then have implementations that
 * adapt generated code to this interface. This allows us to have a
 * single client implementation, using a single representation of the
 * request, which could be backed by a generated message that uses either
 * the standard or the lite runtime.
 *
 * Unfortunately, the standard and lite runtimes are incompatible so we
 * can't directly use either of them as the singular representation that
 * the client implementation uses. So this abstraction is needed so the
 * same client code can be used for both runtimes.
 */
interface ClientCompatRequest {
    val testName: String
    val service: String
    val method: String
    val host: String
    val port: Int
    val serverTlsCert: ByteString
    val clientTlsCreds: TlsCreds?
    val timeoutMs: Int
    val requestDelayMs: Int
    val useGetHttpMethod: Boolean
    val httpVersion: HttpVersion
    val protocol: NetworkProtocol
    val codec: Codec
    val compression: Compression
    val streamType: StreamType
    val requestHeaders: Headers
    val requestMessages: List<AnyMessage>
    val cancel: Cancel?

    interface TlsCreds {
        val cert: ByteString
        val key: ByteString
    }

    sealed class Cancel {
        class BeforeCloseSend : Cancel()
        class AfterCloseSendMs(val millis: Int) : Cancel()
        class AfterNumResponses(val num: Int) : Cancel()
    }

    enum class HttpVersion {
        HTTP_1_1,
        HTTP_2,
    }

    enum class Codec {
        PROTO,
        JSON,
    }

    enum class Compression {
        IDENTITY,
        GZIP,
    }

    enum class StreamType {
        UNARY,
        CLIENT_STREAM,
        SERVER_STREAM,
        HALF_DUPLEX_BIDI_STREAM,
        FULL_DUPLEX_BIDI_STREAM,
    }
}

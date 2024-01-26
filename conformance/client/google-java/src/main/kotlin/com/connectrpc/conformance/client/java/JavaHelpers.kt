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

package com.connectrpc.conformance.client.java

import com.connectrpc.ConnectException
import com.connectrpc.Headers
import com.connectrpc.SerializationStrategy
import com.connectrpc.conformance.client.adapt.AnyMessage
import com.connectrpc.conformance.client.adapt.ClientCompatRequest
import com.connectrpc.conformance.client.adapt.ClientCompatRequest.HttpVersion
import com.connectrpc.conformance.client.adapt.ClientCompatRequest.TlsCreds
import com.connectrpc.conformance.client.adapt.ClientCompatResponse
import com.connectrpc.conformance.v1.BidiStreamResponse
import com.connectrpc.conformance.v1.ClientCompatProto
import com.connectrpc.conformance.v1.ClientCompatRequest.Cancel.CancelTimingCase
import com.connectrpc.conformance.v1.ClientErrorResult
import com.connectrpc.conformance.v1.ClientResponseResult
import com.connectrpc.conformance.v1.ClientStreamResponse
import com.connectrpc.conformance.v1.Codec
import com.connectrpc.conformance.v1.Compression
import com.connectrpc.conformance.v1.ConfigProto
import com.connectrpc.conformance.v1.ConformancePayload
import com.connectrpc.conformance.v1.Error
import com.connectrpc.conformance.v1.HTTPVersion
import com.connectrpc.conformance.v1.Header
import com.connectrpc.conformance.v1.IdempotentUnaryResponse
import com.connectrpc.conformance.v1.Protocol
import com.connectrpc.conformance.v1.ServerCompatProto
import com.connectrpc.conformance.v1.ServerStreamResponse
import com.connectrpc.conformance.v1.ServiceProto
import com.connectrpc.conformance.v1.StreamType
import com.connectrpc.conformance.v1.SuiteProto
import com.connectrpc.conformance.v1.UnaryResponse
import com.connectrpc.conformance.v1.UnimplementedResponse
import com.connectrpc.extensions.GoogleJavaJSONStrategy
import com.connectrpc.extensions.GoogleJavaProtobufStrategy
import com.connectrpc.protocols.NetworkProtocol
import com.google.protobuf.Any
import com.google.protobuf.ByteString
import com.google.protobuf.MessageLite
import com.google.protobuf.TypeRegistry

class JavaHelpers {
    companion object {
        private const val TYPE_URL_PREFIX = "type.googleapis.com/"

        fun serializationStrategy(codec: ClientCompatRequest.Codec): SerializationStrategy {
            return when (codec) {
                ClientCompatRequest.Codec.PROTO -> GoogleJavaProtobufStrategy()
                ClientCompatRequest.Codec.JSON -> GoogleJavaJSONStrategy(getTypes())
                else -> throw RuntimeException("unsupported codec $codec")
            }
        }

        fun unmarshalRequest(bytes: ByteArray): ClientCompatRequest {
            val msg = com.connectrpc.conformance.v1.ClientCompatRequest.parseFrom(bytes)
            return ClientCompatRequestImpl(msg)
        }

        fun marshalResponse(resp: ClientCompatResponse): ByteArray {
            val builder = com.connectrpc.conformance.v1.ClientCompatResponse
                .newBuilder()
                .setTestName(resp.testName)
            when (val result = resp.result) {
                is ClientCompatResponse.Result.ResponseResult -> {
                    val respBuilder = ClientResponseResult.newBuilder()
                        .addAllResponseHeaders(toProtoHeaders(result.response.headers))
                        .addAllPayloads(toProtoPayloads(result.response.payloads))
                        .addAllResponseTrailers(toProtoHeaders(result.response.trailers))
                        .setNumUnsentRequests(result.response.numUnsentRequests)
                    val err = result.response.error
                    if (err != null) {
                        respBuilder.setError(toProtoError(err))
                    }
                    val respMsg = respBuilder.build()
                    result.response.raw = respMsg
                    builder.setResponse(respMsg)
                }
                is ClientCompatResponse.Result.ErrorResult -> {
                    builder.setError(
                        ClientErrorResult.newBuilder()
                            .setMessage(result.error),
                    )
                }
            }
            return builder.build().toByteArray()
        }

        fun extractPayload(response: MessageLite): MessageLite {
            return when (response) {
                is UnaryResponse -> response.payload
                is IdempotentUnaryResponse -> response.payload
                is UnimplementedResponse -> ConformancePayload.getDefaultInstance()
                is ClientStreamResponse -> response.payload
                is ServerStreamResponse -> response.payload
                is BidiStreamResponse -> response.payload
                else -> throw RuntimeException("don't know how to extract payload from ${response::class.qualifiedName}")
            }
        }

        private fun fromProtoHeaders(headers: List<Header>): Headers {
            return headers.groupingBy(Header::getName).aggregate { _: String, accumulator: List<String>?, element: Header, _: Boolean ->
                accumulator?.plus(element.valueList) ?: element.valueList
            }
        }

        private fun toProtoHeaders(headers: Headers): List<Header> {
            return headers.map {
                Header.newBuilder()
                    .setName(it.key)
                    .addAllValue(it.value)
                    .build()
            }
        }

        private fun toProtoPayloads(payloads: List<MessageLite>): List<ConformancePayload> {
            return payloads.map {
                if (it is ConformancePayload) {
                    it
                } else {
                    ConformancePayload.parseFrom(it.toByteArray())
                }
            }
        }

        private fun toProtoError(ex: ConnectException): Error {
            return Error.newBuilder()
                .setCode(ex.code.value)
                .setMessage(ex.message ?: ex.code.codeName)
                .addAllDetails(
                    ex.details.map {
                        Any.newBuilder()
                            .setTypeUrl(toTypeUrl(it.type))
                            .setValue(ByteString.copyFrom(it.payload.toByteArray()))
                            .build()
                    },
                )
                .build()
        }

        private fun toTypeUrl(typeName: String): String {
            return if (typeName.contains('/')) typeName else TYPE_URL_PREFIX + typeName
        }

        private fun getTypes(): TypeRegistry {
            return TypeRegistry.newBuilder()
                .add(ClientCompatProto.getDescriptor().messageTypes)
                .add(ConfigProto.getDescriptor().messageTypes)
                .add(ServerCompatProto.getDescriptor().messageTypes)
                .add(ServiceProto.getDescriptor().messageTypes)
                .add(SuiteProto.getDescriptor().messageTypes)
                .build()
        }
    }

    private class ClientCompatRequestImpl(
        private val msg: com.connectrpc.conformance.v1.ClientCompatRequest,
    ) : ClientCompatRequest {
        override val raw: kotlin.Any
            get() = msg
        override val testName: String
            get() = msg.testName
        override val service: String
            get() = msg.service
        override val method: String
            get() = msg.method
        override val host: String
            get() = msg.host
        override val port: Int
            get() = msg.port
        override val serverTlsCert: ByteString
            get() = msg.serverTlsCert
        override val clientTlsCreds: TlsCreds?
            get() = if (msg.hasClientTlsCreds()) TlsCredsImpl(msg.clientTlsCreds) else null
        override val receiveLimitBytes: Int
            get() = msg.messageReceiveLimit
        override val timeoutMs: Int
            get() = msg.timeoutMs
        override val requestDelayMs: Int
            get() = msg.requestDelayMs
        override val useGetHttpMethod: Boolean
            get() = msg.useGetHttpMethod
        override val httpVersion: HttpVersion
            get() = when (msg.httpVersion) {
                HTTPVersion.HTTP_VERSION_1 -> HttpVersion.HTTP_1_1
                HTTPVersion.HTTP_VERSION_2 -> HttpVersion.HTTP_2
                else -> throw RuntimeException("unsupported HTTP version: ${msg.httpVersion}")
            }
        override val protocol: NetworkProtocol
            get() = when (msg.protocol) {
                Protocol.PROTOCOL_CONNECT -> NetworkProtocol.CONNECT
                Protocol.PROTOCOL_GRPC -> NetworkProtocol.GRPC
                Protocol.PROTOCOL_GRPC_WEB -> NetworkProtocol.GRPC_WEB
                else -> throw RuntimeException("unsupported protocol: ${msg.protocol}")
            }
        override val codec: ClientCompatRequest.Codec
            get() = when (msg.codec) {
                Codec.CODEC_PROTO -> ClientCompatRequest.Codec.PROTO
                Codec.CODEC_JSON -> ClientCompatRequest.Codec.JSON
                else -> throw RuntimeException("unsupported codec: ${msg.codec}")
            }
        override val compression: ClientCompatRequest.Compression
            get() = when (msg.compression) {
                Compression.COMPRESSION_IDENTITY, Compression.COMPRESSION_UNSPECIFIED -> ClientCompatRequest.Compression.IDENTITY
                Compression.COMPRESSION_GZIP -> ClientCompatRequest.Compression.GZIP
                else -> throw RuntimeException("unsupported compression: ${msg.compression}")
            }
        override val streamType: ClientCompatRequest.StreamType
            get() = when (msg.streamType) {
                StreamType.STREAM_TYPE_UNARY -> ClientCompatRequest.StreamType.UNARY
                StreamType.STREAM_TYPE_CLIENT_STREAM -> ClientCompatRequest.StreamType.CLIENT_STREAM
                StreamType.STREAM_TYPE_SERVER_STREAM -> ClientCompatRequest.StreamType.SERVER_STREAM
                StreamType.STREAM_TYPE_HALF_DUPLEX_BIDI_STREAM -> ClientCompatRequest.StreamType.HALF_DUPLEX_BIDI_STREAM
                StreamType.STREAM_TYPE_FULL_DUPLEX_BIDI_STREAM -> ClientCompatRequest.StreamType.FULL_DUPLEX_BIDI_STREAM
                else -> throw RuntimeException("unsupported stream type: ${msg.streamType}")
            }
        override val requestHeaders: Headers
            get() = fromProtoHeaders(msg.requestHeadersList)
        override val requestMessages: List<AnyMessage>
            get() = msg.requestMessagesList.map {
                AnyMessage(it.typeUrl, it.value)
            }
        override val cancel: ClientCompatRequest.Cancel?
            get() = when (msg.cancel.cancelTimingCase) {
                CancelTimingCase.CANCELTIMING_NOT_SET, null ->
                    null
                CancelTimingCase.BEFORE_CLOSE_SEND ->
                    ClientCompatRequest.Cancel.BeforeCloseSend()
                CancelTimingCase.AFTER_CLOSE_SEND_MS ->
                    ClientCompatRequest.Cancel.AfterCloseSendMs(msg.cancel.afterCloseSendMs)
                CancelTimingCase.AFTER_NUM_RESPONSES ->
                    ClientCompatRequest.Cancel.AfterNumResponses(msg.cancel.afterNumResponses)
            }
    }

    private class TlsCredsImpl(
        private val msg: com.connectrpc.conformance.v1.ClientCompatRequest.TLSCreds,
    ) : TlsCreds {
        override val cert: ByteString
            get() = msg.cert
        override val key: ByteString
            get() = msg.key
    }
}

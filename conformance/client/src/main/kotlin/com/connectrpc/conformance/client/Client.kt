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

package com.connectrpc.conformance.client

import com.connectrpc.ProtocolClientConfig
import com.connectrpc.RequestCompression
import com.connectrpc.ResponseMessage
import com.connectrpc.SerializationStrategy
import com.connectrpc.compression.GzipCompressionPool
import com.connectrpc.conformance.client.adapt.BidiStreamClient
import com.connectrpc.conformance.client.adapt.ClientStreamClient
import com.connectrpc.conformance.client.adapt.Invoker
import com.connectrpc.conformance.client.adapt.ServerStreamClient
import com.connectrpc.conformance.client.adapt.UnaryClient
import com.connectrpc.impl.ProtocolClient
import com.connectrpc.lite.connectrpc.conformance.v1.ClientCompatRequest
import com.connectrpc.lite.connectrpc.conformance.v1.ClientResponseResult
import com.connectrpc.lite.connectrpc.conformance.v1.Codec
import com.connectrpc.lite.connectrpc.conformance.v1.Compression
import com.connectrpc.lite.connectrpc.conformance.v1.HTTPVersion
import com.connectrpc.lite.connectrpc.conformance.v1.Protocol
import com.connectrpc.lite.connectrpc.conformance.v1.StreamType
import com.connectrpc.okhttp.ConnectOkHttpClient
import com.connectrpc.protocols.GETConfiguration
import com.connectrpc.protocols.NetworkProtocol
import com.google.protobuf.MessageLite
import okhttp3.OkHttpClient
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import java.security.KeyFactory
import java.security.KeyPair
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.time.Duration
import java.util.Base64
import kotlin.reflect.cast
import kotlin.reflect.safeCast

class Client(
    private val invokerFactory: (ProtocolClient) -> Invoker,
    private val serializationFactory: (Codec) -> SerializationStrategy,
    private val invokeStyle: UnaryClient.InvokeStyle,
) {
    suspend fun handle(req: ClientCompatRequest): ClientResponseResult {
        val invoker = invokerFactory(getProtocolClient(req))
        val service = req.service.orEmpty()
        if (service != "connectrpc.conformance.v1.ConformanceService") {
            throw RuntimeException("service $service is not known")
        }

        return when (val method = req.method.orEmpty()) {
            "Unary" -> handleUnary(invoker.unaryClient(), req)
            // TODO: IdempotentUnary
            "Unimplemented" -> handleUnary(invoker.unimplementedClient(), req)
            "ClientStream" -> handleClient(invoker.clientStreamClient(), req)
            "ServerStream" -> handleServer(invoker.serverStreamClient(), req)
            "BidiStream" -> if (req.streamType == StreamType.STREAM_TYPE_FULL_DUPLEX_BIDI_STREAM) {
                handleFullDuplexBidi(invoker.bidiStreamClient(), req)
            } else {
                handleHalfDuplexBidi(invoker.bidiStreamClient(), req)
            }
            else -> throw RuntimeException("method $method is not known")
        }
    }

    private suspend fun <Req : MessageLite, Resp : MessageLite> handleUnary(
        client: UnaryClient<Req, Resp>,
        req: ClientCompatRequest,
    ): ClientResponseResult {
        TODO("implement me")
    }

    private suspend fun <Req : MessageLite, Resp : MessageLite> handleClient(
        client: ClientStreamClient<Req, Resp>,
        req: ClientCompatRequest,
    ): ClientResponseResult {
        TODO("implement me")
    }

    private suspend fun <Req : MessageLite, Resp : MessageLite> handleServer(
        client: ServerStreamClient<Req, Resp>,
        req: ClientCompatRequest,
    ): ClientResponseResult {
        TODO("implement me")
    }

    private suspend fun <Req : MessageLite, Resp : MessageLite> handleHalfDuplexBidi(
        client: BidiStreamClient<Req, Resp>,
        req: ClientCompatRequest,
    ): ClientResponseResult {
        TODO("implement me")
    }

    private suspend fun <Req : MessageLite, Resp : MessageLite> handleFullDuplexBidi(
        client: BidiStreamClient<Req, Resp>,
        req: ClientCompatRequest,
    ): ClientResponseResult {
        TODO("implement me")
    }

    private fun getProtocolClient(req: ClientCompatRequest): ProtocolClient {
        // TODO: cache/re-use clients instead of creating a new one for every request
        val serializationStrategy = serializationFactory(req.codec)
        val useTls = !req.serverTlsCert.isEmpty
        val scheme = if (useTls) "https" else "http"
        val host = "$scheme://${req.host}:${req.port}"
        var clientBuilder = OkHttpClient.Builder()
            .protocols(listOf(asOkHttpProtocol(req.httpVersion, useTls)))
            .connectTimeout(Duration.ofMinutes(1))
        if (useTls) {
            val certs = certs(req)
            clientBuilder = clientBuilder.sslSocketFactory(certs.sslSocketFactory(), certs.trustManager)
        }
        if (req.hasTimeoutMs()) {
            clientBuilder = clientBuilder.callTimeout(Duration.ofMillis(req.timeoutMs.toLong()))
        }

        val getConfig = if (req.useGetHttpMethod) GETConfiguration.Enabled else GETConfiguration.Disabled
        val requestCompression =
            if (req.compression == Compression.COMPRESSION_GZIP) {
                RequestCompression(10, GzipCompressionPool)
            } else {
                null
            }
        val compressionPools =
            if (req.compression == Compression.COMPRESSION_GZIP) {
                listOf(GzipCompressionPool)
            } else {
                emptyList()
            }
        return ProtocolClient(
            httpClient = ConnectOkHttpClient(clientBuilder.build()),
            ProtocolClientConfig(
                host = host,
                serializationStrategy = serializationStrategy,
                networkProtocol = asNetworkProtocol(req.protocol),
                getConfiguration = getConfig,
                requestCompression = requestCompression,
                compressionPools = compressionPools,
            ),
        )
    }

    private fun asNetworkProtocol(protocol: Protocol): NetworkProtocol {
        return when (protocol) {
            Protocol.PROTOCOL_CONNECT -> NetworkProtocol.CONNECT
            Protocol.PROTOCOL_GRPC -> NetworkProtocol.GRPC
            Protocol.PROTOCOL_GRPC_WEB -> NetworkProtocol.GRPC_WEB
            else -> throw RuntimeException("unsupported protocol: $protocol")
        }
    }

    private fun asOkHttpProtocol(httpVersion: HTTPVersion, useTls: Boolean): okhttp3.Protocol {
        return when (httpVersion) {
            HTTPVersion.HTTP_VERSION_1 -> okhttp3.Protocol.HTTP_1_1
            HTTPVersion.HTTP_VERSION_2 -> if (useTls) okhttp3.Protocol.HTTP_2 else okhttp3.Protocol.H2_PRIOR_KNOWLEDGE
            else -> throw RuntimeException("unsupported HTTP version: $httpVersion")
        }
    }

    private fun certs(req: ClientCompatRequest): HandshakeCertificates {
        val certificateAuthority = req.serverTlsCert.newInput().use { stream ->
            CertificateFactory.getInstance("X.509").generateCertificate(stream) as X509Certificate
        }
        val result = HandshakeCertificates.Builder()
            .addTrustedCertificate(certificateAuthority)

        if (!req.hasClientTlsCreds()) {
            return result.build()
        }

        val certificate = req.clientTlsCreds.cert.newInput().use { stream ->
            CertificateFactory.getInstance("X.509").generateCertificate(stream) as X509Certificate
        }
        val publicKey = certificate.publicKey as RSAPublicKey
        val privateKeyBytes = req.clientTlsCreds.key.newInput().bufferedReader().use { stream ->
            val lines = stream.readLines().toMutableList()
            // Remove BEGIN RSA PRIVATE KEY / END RSA PRIVATE KEY lines
            lines.removeFirst()
            lines.removeLast()
            Base64.getDecoder().decode(lines.joinToString(separator = ""))
        }
        val privateKey = KeyFactory.getInstance("RSA")
            .generatePrivate(PKCS8EncodedKeySpec(privateKeyBytes)) as RSAPrivateKey
        if (publicKey.modulus != privateKey.modulus) {
            throw Exception("key does not match cert") // or other error handling
        }
        return result
            .heldCertificate(HeldCertificate(KeyPair(publicKey, privateKey), certificate))
            .build()
    }

    private fun <From : MessageLite, To : MessageLite> convert(
        from: From,
        template: To,
    ): To {
        val clazz = template::class
        return clazz.safeCast(from)
            ?: clazz.cast(
                template
                    .newBuilderForType()
                    .mergeFrom(from.toByteString())
                    .build(),
            )
    }

    private fun <From : MessageLite, To : MessageLite> convert(
        from: ResponseMessage<From>,
        template: To,
    ): ResponseMessage<To> {
        return when (from) {
            is ResponseMessage.Success -> {
                ResponseMessage.Success(
                    message = convert(from.message, template),
                    code = from.code,
                    headers = from.headers,
                    trailers = from.trailers,
                )
            }
            is ResponseMessage.Failure -> {
                // Value does not actually contain a reference
                // to response type, so we can just cast it.
                @Suppress("UNCHECKED_CAST")
                from as ResponseMessage<To>
            }
        }
    }
}

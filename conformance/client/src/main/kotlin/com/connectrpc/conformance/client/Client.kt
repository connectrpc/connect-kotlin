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
import com.connectrpc.SerializationStrategy
import com.connectrpc.compression.GzipCompressionPool
import com.connectrpc.conformance.client.adapt.AnyMessage
import com.connectrpc.conformance.client.adapt.BidiStreamClient
import com.connectrpc.conformance.client.adapt.ClientCompatRequest
import com.connectrpc.conformance.client.adapt.ClientCompatRequest.Codec
import com.connectrpc.conformance.client.adapt.ClientCompatRequest.Compression
import com.connectrpc.conformance.client.adapt.ClientCompatRequest.HttpVersion
import com.connectrpc.conformance.client.adapt.ClientCompatRequest.StreamType
import com.connectrpc.conformance.client.adapt.ClientResponseResult
import com.connectrpc.conformance.client.adapt.ClientStreamClient
import com.connectrpc.conformance.client.adapt.Invoker
import com.connectrpc.conformance.client.adapt.ServerStreamClient
import com.connectrpc.conformance.client.adapt.UnaryClient
import com.connectrpc.impl.ProtocolClient
import com.connectrpc.okhttp.ConnectOkHttpClient
import com.connectrpc.protocols.GETConfiguration
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

/**
 * The conformance client. This contains the logic for invoking an
 * RPC and returning a representation of its result.
 */
class Client(
    private val invokerFactory: (ProtocolClient) -> Invoker,
    private val serializationFactory: (Codec) -> SerializationStrategy,
    private val invokeStyle: UnaryClient.InvokeStyle,
    private val payloadExtractor: (MessageLite) -> MessageLite,
) {
    companion object {
        private const val CONFORMANCE_SERVICE_NAME = "connectrpc.conformance.v1.ConformanceService"
        private const val UNARY_METHOD_NAME = "Unary"
        private const val IDEMPOTENT_UNARY_METHOD_NAME = "IdempotentUnary"
        private const val UNIMPLEMENTED_METHOD_NAME = "Unimplemented"
        private const val CLIENT_STREAM_METHOD_NAME = "ClientStream"
        private const val SERVER_STREAM_METHOD_NAME = "ServerStream"
        private const val BIDI_STREAM_METHOD_NAME = "BidiStream"

        private const val UNARY_REQUEST_NAME = "connectrpc.conformance.v1.UnaryRequest"
        private const val IDEMPOTENT_UNARY_REQUEST_NAME = "connectrpc.conformance.v1.IdempotentUnaryRequest"
        private const val UNIMPLEMENTED_REQUEST_NAME = "connectrpc.conformance.v1.UnimplementedRequest"
    }

    suspend fun handle(req: ClientCompatRequest): ClientResponseResult {
        val (httpClient, protocolClient) = getClient(req)
        try {
            val invoker = invokerFactory(protocolClient)
            val service = req.service
            if (service != CONFORMANCE_SERVICE_NAME) {
                throw RuntimeException("service $service is not known")
            }

            return when (req.method) {
                UNARY_METHOD_NAME -> handleUnary(invoker.unaryClient(), UNARY_REQUEST_NAME, req)
                IDEMPOTENT_UNARY_METHOD_NAME -> handleUnary(invoker.idempotentUnaryClient(), IDEMPOTENT_UNARY_REQUEST_NAME, req)
                UNIMPLEMENTED_METHOD_NAME -> handleUnary(invoker.unimplementedClient(), UNIMPLEMENTED_REQUEST_NAME, req)
                CLIENT_STREAM_METHOD_NAME -> handleClient(invoker.clientStreamClient(), req)
                SERVER_STREAM_METHOD_NAME -> handleServer(invoker.serverStreamClient(), req)
                BIDI_STREAM_METHOD_NAME -> handleBidi(invoker.bidiStreamClient(), req)
                else -> throw RuntimeException("method ${req.method} is not known")
            }
        } finally {
            // Clean-up HTTP client.
            httpClient.connectionPool.evictAll()
            httpClient.dispatcher.executorService.shutdown()
        }
    }

    private suspend fun <
        Req : MessageLite,
        Resp : MessageLite,
        > handleUnary(
        client: UnaryClient<Req, Resp>,
        requestType: String,
        req: ClientCompatRequest,
    ): ClientResponseResult {
        if (req.streamType != StreamType.UNARY) {
            throw RuntimeException("specified method ${req.method} is unary but stream type indicates ${req.streamType}")
        }
        TODO("implement me")
    }

    private suspend fun <Req : MessageLite, Resp : MessageLite> handleClient(
        client: ClientStreamClient<Req, Resp>,
        req: ClientCompatRequest,
    ): ClientResponseResult {
        if (req.streamType != StreamType.CLIENT_STREAM) {
            throw RuntimeException("specified method ${req.method} is client-stream but stream type indicates ${req.streamType}")
        }
        TODO("implement me")
    }

    private suspend fun <Req : MessageLite, Resp : MessageLite> handleServer(
        client: ServerStreamClient<Req, Resp>,
        req: ClientCompatRequest,
    ): ClientResponseResult {
        if (req.streamType != StreamType.SERVER_STREAM) {
            throw RuntimeException("specified method ${req.method} is server-stream but stream type indicates ${req.streamType}")
        }
        TODO("implement me")
    }

    private suspend fun <Req : MessageLite, Resp : MessageLite> handleBidi(
        client: BidiStreamClient<Req, Resp>,
        req: ClientCompatRequest,
    ): ClientResponseResult {
        return when (req.streamType) {
            StreamType.HALF_DUPLEX_BIDI_STREAM ->
                handleHalfDuplexBidi(client, req)
            StreamType.FULL_DUPLEX_BIDI_STREAM ->
                handleFullDuplexBidi(client, req)
            else ->
                throw RuntimeException("specified method ${req.method} is bidi-stream but stream type indicates ${req.streamType}")
        }
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

    private fun getClient(req: ClientCompatRequest): Pair<OkHttpClient, ProtocolClient> {
        // TODO: cache/re-use clients instead of creating a new one for every request
        val serializationStrategy = serializationFactory(req.codec)
        val useTls = !req.serverTlsCert.isEmpty
        val scheme = if (useTls) "https" else "http"
        val host = "$scheme://${req.host}:${req.port}"
        var clientBuilder = OkHttpClient.Builder()
            .protocols(asOkHttpProtocols(req.httpVersion, useTls))
            .connectTimeout(Duration.ofMinutes(1))
        if (useTls) {
            val certs = certs(req)
            clientBuilder = clientBuilder.sslSocketFactory(certs.sslSocketFactory(), certs.trustManager)
        }
        if (req.timeoutMs != 0) {
            clientBuilder = clientBuilder.callTimeout(Duration.ofMillis(req.timeoutMs.toLong()))
        }

        val getConfig = if (req.useGetHttpMethod) GETConfiguration.Enabled else GETConfiguration.Disabled
        val requestCompression =
            if (req.compression == Compression.GZIP) {
                RequestCompression(0, GzipCompressionPool)
            } else {
                null
            }
        val compressionPools =
            if (req.compression == Compression.GZIP) {
                listOf(GzipCompressionPool)
            } else {
                emptyList()
            }
        val httpClient = clientBuilder.build()
        return Pair(
            httpClient,
            ProtocolClient(
                httpClient = ConnectOkHttpClient(httpClient),
                ProtocolClientConfig(
                    host = host,
                    serializationStrategy = serializationStrategy,
                    networkProtocol = req.protocol,
                    getConfiguration = getConfig,
                    requestCompression = requestCompression,
                    compressionPools = compressionPools,
                ),
            ),
        )
    }

    private fun asOkHttpProtocols(httpVersion: HttpVersion, useTls: Boolean): List<okhttp3.Protocol> {
        return when (httpVersion) {
            HttpVersion.HTTP_1_1 -> listOf(okhttp3.Protocol.HTTP_1_1)
            HttpVersion.HTTP_2 ->
                if (useTls) {
                    // okhttp *requires* that protocols contains HTTP_1_1
                    // or H2_PRIOR_KNOWLEDGE. So we leave 1.1 in here, but
                    // expect HTTP/2 to always be used in practice since it
                    // should be negotiated during TLS handshake,
                    listOf(okhttp3.Protocol.HTTP_2, okhttp3.Protocol.HTTP_1_1)
                } else {
                    listOf(okhttp3.Protocol.H2_PRIOR_KNOWLEDGE)
                }
        }
    }

    private fun certs(req: ClientCompatRequest): HandshakeCertificates {
        val certificateAuthority = req.serverTlsCert.newInput().use { stream ->
            CertificateFactory.getInstance("X.509").generateCertificate(stream) as X509Certificate
        }
        val result = HandshakeCertificates.Builder()
            .addTrustedCertificate(certificateAuthority)

        val creds = req.clientTlsCreds ?: return result.build()

        val certificate = creds.cert.newInput().use { stream ->
            CertificateFactory.getInstance("X.509").generateCertificate(stream) as X509Certificate
        }
        val publicKey = certificate.publicKey as RSAPublicKey
        val privateKeyBytes = creds.key.newInput().bufferedReader().use { stream ->
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

    private fun <M : MessageLite> fromAny(
        any: AnyMessage,
        template: M,
        typeName: String,
    ): M {
        val pos = any.typeUrl.lastIndexOf('/')
        val actualTypeName = any.typeUrl.substring(pos + 1)
        if (actualTypeName != typeName) {
            throw RuntimeException("expecting request message to be $typeName, instead got $actualTypeName")
        }
        val msgClass = template::class
        return msgClass.cast(
            template.newBuilderForType().mergeFrom(any.value).build(),
        )
    }
}

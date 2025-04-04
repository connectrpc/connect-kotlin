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

package com.connectrpc.conformance.client

import com.connectrpc.Code
import com.connectrpc.ConnectException
import com.connectrpc.Headers
import com.connectrpc.ProtocolClientConfig
import com.connectrpc.RequestCompression
import com.connectrpc.ResponseMessage
import com.connectrpc.SerializationStrategy
import com.connectrpc.asConnectException
import com.connectrpc.compression.GzipCompressionPool
import com.connectrpc.conformance.client.ClientArgs.UnaryInvokeStyle
import com.connectrpc.conformance.client.adapt.AnyMessage
import com.connectrpc.conformance.client.adapt.BidiStreamClient
import com.connectrpc.conformance.client.adapt.ClientCompatRequest
import com.connectrpc.conformance.client.adapt.ClientCompatRequest.Cancel
import com.connectrpc.conformance.client.adapt.ClientCompatRequest.Codec
import com.connectrpc.conformance.client.adapt.ClientCompatRequest.Compression
import com.connectrpc.conformance.client.adapt.ClientCompatRequest.HttpVersion
import com.connectrpc.conformance.client.adapt.ClientCompatRequest.StreamType
import com.connectrpc.conformance.client.adapt.ClientResponseResult
import com.connectrpc.conformance.client.adapt.ClientStreamClient
import com.connectrpc.conformance.client.adapt.Invoker
import com.connectrpc.conformance.client.adapt.ResponseStream
import com.connectrpc.conformance.client.adapt.ServerStreamClient
import com.connectrpc.conformance.client.adapt.UnaryClient
import com.connectrpc.http.Cancelable
import com.connectrpc.http.HTTPClientInterface
import com.connectrpc.http.Timeout
import com.connectrpc.impl.ProtocolClient
import com.connectrpc.okhttp.ConnectOkHttpClient
import com.connectrpc.protocols.GETConfiguration
import com.google.protobuf.MessageLite
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
import kotlin.time.DurationUnit
import kotlin.time.toDuration

/**
 * The conformance client. This contains the logic for invoking an
 * RPC and returning a representation of its result.
 */
class Client(
    private val args: ClientArgs,
    private val invokerFactory: (ProtocolClient) -> Invoker,
    private val serializationFactory: (Codec) -> SerializationStrategy,
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
        private const val CLIENT_STREAM_REQUEST_NAME = "connectrpc.conformance.v1.ClientStreamRequest"
        private const val SERVER_STREAM_REQUEST_NAME = "connectrpc.conformance.v1.ServerStreamRequest"
        private const val BIDI_STREAM_REQUEST_NAME = "connectrpc.conformance.v1.BidiStreamRequest"
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
        if (req.requestMessages.size != 1) {
            throw RuntimeException("unary calls should indicate exactly one request message, got ${req.requestMessages.size}")
        }
        if (req.cancel != null && req.cancel !is Cancel.AfterCloseSendMs) {
            throw RuntimeException("unary calls can only support 'AfterCloseSendMs' cancellation field, instead got ${req.cancel!!::class.simpleName}")
        }
        val msg = fromAny(req.requestMessages[0], client.reqTemplate, requestType)
        val resp = CompletableDeferred<ResponseMessage<Resp>>()

        return coroutineScope {
            val canceler: Cancelable
            when (args.unaryInvokeStyle) {
                UnaryInvokeStyle.CALLBACK -> {
                    canceler = client.execute(msg, req.requestHeaders, resp::complete)
                }

                UnaryInvokeStyle.SUSPEND -> {
                    val job = launch {
                        try {
                            resp.complete(client.execute(msg, req.requestHeaders))
                        } catch (ex: Throwable) {
                            val code = if (ex is CancellationException) {
                                Code.CANCELED
                            } else {
                                Code.UNKNOWN
                            }
                            resp.complete(
                                ResponseMessage.Failure(
                                    cause = ConnectException(code, exception = ex),
                                    headers = emptyMap(),
                                    trailers = emptyMap(),
                                ),
                            )
                        }
                    }
                    canceler = { job.cancel() }
                }

                UnaryInvokeStyle.BLOCKING -> {
                    val call = client.blocking(msg, req.requestHeaders)
                    launch(Dispatchers.IO) {
                        resp.complete(call.execute())
                    }
                    canceler = { call.cancel() }
                }
            }

            when (val cancel = req.cancel) {
                is Cancel.AfterCloseSendMs -> {
                    launch {
                        delay(cancel.millis.toLong())
                        canceler()
                    }
                }

                else -> {
                    // We already validated the case above.
                    // So this case means no cancellation.
                }
            }
            unaryResult(0, resp.await())
        }
    }

    private suspend fun <Req : MessageLite, Resp : MessageLite> handleClient(
        client: ClientStreamClient<Req, Resp>,
        req: ClientCompatRequest,
    ): ClientResponseResult {
        if (req.streamType != StreamType.CLIENT_STREAM) {
            throw RuntimeException("specified method ${req.method} is client-stream but stream type indicates ${req.streamType}")
        }
        if (req.cancel != null &&
            req.cancel !is Cancel.BeforeCloseSend &&
            req.cancel !is Cancel.AfterCloseSendMs
        ) {
            throw RuntimeException("client stream calls can only support `BeforeCloseSend` and 'AfterCloseSendMs' cancellation field, instead got ${req.cancel!!::class.simpleName}")
        }
        return client.execute(req.requestHeaders) { stream ->
            var numUnsent = 0
            for (i in req.requestMessages.indices) {
                if (req.requestDelayMs > 0) {
                    delay(req.requestDelayMs.toLong())
                }
                val msg = fromAny(req.requestMessages[i], client.reqTemplate, CLIENT_STREAM_REQUEST_NAME)
                try {
                    stream.send(msg)
                } catch (ex: Exception) {
                    args.verbose.verbosity(2) {
                        println("Failed to send request message:")
                        indent().println(ex.stackTraceToString())
                    }
                    numUnsent = req.requestMessages.size - i
                    break
                }
            }
            when (val cancel = req.cancel) {
                is Cancel.BeforeCloseSend -> {
                    stream.close()
                }

                is Cancel.AfterCloseSendMs -> {
                    launch {
                        delay(cancel.millis.toLong())
                        stream.close()
                    }
                }

                else -> {
                    // We already validated the case above.
                    // So this case means no cancellation.
                }
            }
            unaryResult(numUnsent, stream.closeAndReceive())
        }
    }

    private suspend fun <Req : MessageLite, Resp : MessageLite> handleServer(
        client: ServerStreamClient<Req, Resp>,
        req: ClientCompatRequest,
    ): ClientResponseResult {
        if (req.streamType != StreamType.SERVER_STREAM) {
            throw RuntimeException("specified method ${req.method} is server-stream but stream type indicates ${req.streamType}")
        }
        if (req.requestMessages.size != 1) {
            throw RuntimeException("server-stream calls should indicate exactly one request message, got ${req.requestMessages.size}")
        }
        if (req.cancel != null &&
            req.cancel !is Cancel.AfterCloseSendMs &&
            req.cancel !is Cancel.AfterNumResponses
        ) {
            throw RuntimeException("server stream calls can only support `AfterCloseSendMs` and 'AfterNumResponses' cancellation field, instead got ${req.cancel!!::class.simpleName}")
        }
        val msg = fromAny(req.requestMessages[0], client.reqTemplate, SERVER_STREAM_REQUEST_NAME)
        var sent = false
        try {
            return client.execute(msg, req.requestHeaders) { stream ->
                sent = true
                val cancel = req.cancel
                if (cancel is Cancel.AfterCloseSendMs) {
                    delay(cancel.millis.toLong())
                    stream.close()
                }
                streamResult(0, stream, cancel)
            }
        } catch (ex: Throwable) {
            if (!sent) {
                return ClientResponseResult(
                    error = asConnectException(ex),
                    numUnsentRequests = 1,
                )
            }
            throw ex
        }
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
        return client.execute(req.requestHeaders) { stream ->
            var numUnsent = 0
            for (i in req.requestMessages.indices) {
                if (req.requestDelayMs > 0) {
                    delay(req.requestDelayMs.toLong())
                }
                val msg = fromAny(req.requestMessages[i], client.reqTemplate, BIDI_STREAM_REQUEST_NAME)
                try {
                    stream.requests.send(msg)
                } catch (ex: Exception) {
                    args.verbose.verbosity(2) {
                        println("Failed to send request message:")
                        indent().println(ex.stackTraceToString())
                    }
                    numUnsent = req.requestMessages.size - i
                    break
                }
            }
            val cancel = req.cancel
            when (cancel) {
                is Cancel.BeforeCloseSend -> {
                    stream.close() // cancel
                    stream.requests.close() // close send
                }

                is Cancel.AfterCloseSendMs -> {
                    stream.requests.close() // close send
                    delay(cancel.millis.toLong())
                    stream.close() // cancel
                }

                else -> {
                    stream.requests.close() // close send
                }
            }
            streamResult(numUnsent, stream.responses, cancel)
        }
    }

    private suspend fun <Req : MessageLite, Resp : MessageLite> handleFullDuplexBidi(
        client: BidiStreamClient<Req, Resp>,
        req: ClientCompatRequest,
    ): ClientResponseResult {
        return client.execute(req.requestHeaders) { stream ->
            val cancel = req.cancel
            val payloads: MutableList<MessageLite> = mutableListOf()
            for (i in req.requestMessages.indices) {
                if (req.requestDelayMs > 0) {
                    delay(req.requestDelayMs.toLong())
                }
                val msg = fromAny(req.requestMessages[i], client.reqTemplate, BIDI_STREAM_REQUEST_NAME)
                try {
                    stream.requests.send(msg)
                } catch (ex: Exception) {
                    args.verbose.verbosity(2) {
                        println("Failed to send request message:")
                        indent().println(ex.stackTraceToString())
                    }
                    // Ignore. We should see it again below when we receive the response.
                }

                // In full-duplex mode, we read the response after writing request,
                // to interleave the requests and responses.
                if (i == 0 && cancel is Cancel.AfterNumResponses && cancel.num == 0) {
                    stream.close()
                }
                try {
                    val resp = stream.responses.receive()
                    payloads.add(payloadExtractor(resp))
                    if (cancel is Cancel.AfterNumResponses && cancel.num == payloads.size) {
                        stream.close()
                    }
                } catch (ex: ConnectException) {
                    return@execute ClientResponseResult(
                        headers = stream.responses.headers(),
                        payloads = payloads,
                        error = ex,
                        trailers = ex.metadata,
                        numUnsentRequests = req.requestMessages.size - i,
                    )
                }
            }
            when (cancel) {
                is Cancel.BeforeCloseSend -> {
                    stream.close() // cancel
                    stream.requests.close() // close send
                }

                is Cancel.AfterCloseSendMs -> {
                    stream.requests.close() // close send
                    delay(cancel.millis.toLong())
                    stream.close() // cancel
                }

                else -> {
                    stream.requests.close() // close send
                }
            }

            // Drain the response, in case there are any other messages.
            var connEx: ConnectException? = null
            var trailers: Headers
            try {
                for (resp in stream.responses) {
                    payloads.add(payloadExtractor(resp))
                    if (cancel is Cancel.AfterNumResponses && cancel.num == payloads.size) {
                        stream.close()
                    }
                }
                trailers = stream.responses.trailers()
            } catch (ex: ConnectException) {
                connEx = ex
                trailers = ex.metadata
            }
            ClientResponseResult(
                headers = stream.responses.headers(),
                payloads = payloads,
                error = connEx,
                trailers = trailers,
            )
        }
    }

    private fun unaryResult(numUnsent: Int, result: ResponseMessage<out MessageLite>): ClientResponseResult {
        return when (result) {
            is ResponseMessage.Success -> {
                ClientResponseResult(
                    headers = result.headers,
                    payloads = listOf(payloadExtractor(result.message)),
                    trailers = result.trailers,
                    numUnsentRequests = numUnsent,
                )
            }

            is ResponseMessage.Failure -> {
                ClientResponseResult(
                    headers = result.headers,
                    error = result.cause,
                    trailers = result.trailers,
                    numUnsentRequests = numUnsent,
                )
            }
        }
    }

    private suspend fun streamResult(numUnsent: Int, stream: ResponseStream<out MessageLite>, cancel: Cancel?): ClientResponseResult {
        val payloads: MutableList<MessageLite> = mutableListOf()
        var connEx: ConnectException? = null
        var trailers: Headers
        try {
            if (cancel is Cancel.AfterNumResponses && cancel.num == 0) {
                stream.close()
            }
            for (resp in stream) {
                payloads.add(payloadExtractor(resp))
                if (cancel is Cancel.AfterNumResponses && cancel.num == payloads.size) {
                    stream.close()
                }
            }
            trailers = stream.trailers()
        } catch (ex: ConnectException) {
            connEx = ex
            trailers = ex.metadata
        }
        return ClientResponseResult(
            headers = stream.headers(),
            payloads = payloads,
            error = connEx,
            trailers = trailers,
            numUnsentRequests = numUnsent,
        )
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

        args.verbose.withPrefix("okhttp3 events: ").verbosity(4) {
            clientBuilder = clientBuilder.eventListener(OkHttpEventTracer(this))
        }

        if (useTls) {
            val certs = certs(req)
            clientBuilder = clientBuilder.sslSocketFactory(certs.sslSocketFactory(), certs.trustManager)
        }
        // TODO: need to support max receive bytes and use req.receiveLimitBytes
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
        val httpClient = ConnectOkHttpClient.configureClient(clientBuilder).build()
        var connectHttpClient: HTTPClientInterface = ConnectOkHttpClient(httpClient)
        args.verbose.withPrefix("http client interface: ").verbosity(3) {
            connectHttpClient = TracingHTTPClient(connectHttpClient, this)
        }
        var timeoutScheduler = Timeout.DEFAULT_SCHEDULER
        args.verbose.verbosity(3) {
            val verbosePrinter = this
            timeoutScheduler = object : Timeout.Scheduler {
                override fun scheduleTimeout(delay: kotlin.time.Duration, action: Cancelable): Timeout {
                    verbosePrinter.println("Scheduling timeout in $delay...")
                    val timeout = Timeout.DEFAULT_SCHEDULER.scheduleTimeout(delay) {
                        verbosePrinter.println("Timeout elapsed! Cancelling...")
                        action()
                    }
                    return timeout
                }
            }
        }
        return Pair(
            httpClient,
            ProtocolClient(
                httpClient = connectHttpClient,
                ProtocolClientConfig(
                    host = host,
                    serializationStrategy = serializationStrategy,
                    networkProtocol = req.protocol,
                    getConfiguration = getConfig,
                    requestCompression = requestCompression,
                    compressionPools = compressionPools,
                    timeoutScheduler = timeoutScheduler,
                    timeoutOracle = {
                        if (req.timeoutMs == 0) {
                            null
                        } else {
                            req.timeoutMs.toDuration(DurationUnit.MILLISECONDS)
                        }
                    },
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

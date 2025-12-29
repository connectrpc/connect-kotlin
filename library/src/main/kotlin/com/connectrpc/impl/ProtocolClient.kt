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

package com.connectrpc.impl

import com.connectrpc.BidirectionalStreamInterface
import com.connectrpc.ClientOnlyStreamInterface
import com.connectrpc.Code
import com.connectrpc.ConnectException
import com.connectrpc.Headers
import com.connectrpc.MethodSpec
import com.connectrpc.ProtocolClientConfig
import com.connectrpc.ProtocolClientInterface
import com.connectrpc.ResponseMessage
import com.connectrpc.ServerOnlyStreamInterface
import com.connectrpc.StreamResult
import com.connectrpc.StreamType
import com.connectrpc.UnaryBlockingCall
import com.connectrpc.asConnectException
import com.connectrpc.http.Cancelable
import com.connectrpc.http.HTTPClientInterface
import com.connectrpc.http.HTTPRequest
import com.connectrpc.http.HTTPResponse
import com.connectrpc.http.Timeout
import com.connectrpc.http.UnaryHTTPRequest
import com.connectrpc.http.dispatchIn
import com.connectrpc.http.transform
import com.connectrpc.protocols.GETConfiguration
import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.http.appendPathSegments
import io.ktor.http.encodedPath
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okio.Buffer
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.coroutines.resume

/**
 * Concrete implementation of the [ProtocolClientInterface].
 */
@OptIn(ExperimentalAtomicApi::class)
class ProtocolClient(
    // The client to use for performing requests.
    private val httpClient: HTTPClientInterface,
    // The configuration for the ProtocolClient.
    private val config: ProtocolClientConfig,
) : ProtocolClientInterface {

    private val baseUrlWithTrailingSlash: Url = if (config.baseUrl.encodedPath.endsWith('/')) {
        config.baseUrl
    } else {
        URLBuilder(config.baseUrl).apply {
            encodedPath += "/"
        }.build()
    }

    override fun <Input : Any, Output : Any> unary(
        request: Input,
        headers: Headers,
        methodSpec: MethodSpec<Input, Output>,
        onResult: (ResponseMessage<Output>) -> Unit,
    ): Cancelable {
        val serializationStrategy = config.serializationStrategy
        val requestCodec = serializationStrategy.codec(methodSpec.requestClass)
        try {
            val requestMessage = if (config.getConfiguration != GETConfiguration.Disabled) {
                // Use deterministic serialization when GET request configuration is set.
                requestCodec.deterministicSerialize(request)
            } else {
                requestCodec.serialize(request)
            }
            val requestTimeout = config.timeoutOracle(methodSpec)
            val unaryRequest = UnaryHTTPRequest(
                url = urlFromMethodSpec(methodSpec),
                contentType = "application/${requestCodec.encodingName()}",
                timeout = requestTimeout,
                headers = headers,
                methodSpec = methodSpec,
                message = requestMessage,
            )
            val unaryFunc = config.createInterceptorChain()
            val finalRequest = unaryFunc.requestFunction(unaryRequest)
            val timeoutRef = AtomicReference<Timeout?>(null)
            val finalOnResult: (ResponseMessage<Output>) -> Unit = handleResult@{ result ->
                when (result) {
                    is ResponseMessage.Failure -> {
                        val timeout = timeoutRef.load()
                        if (timeout != null) {
                            timeout.cancel()
                            if (result.cause.code == Code.CANCELED && timeout.timedOut) {
                                onResult(
                                    ResponseMessage.Failure(
                                        cause = ConnectException(Code.DEADLINE_EXCEEDED, exception = result.cause),
                                        headers = result.headers,
                                        trailers = result.trailers,
                                    ),
                                )
                                return@handleResult
                            }
                        }
                        onResult(result)
                    }

                    else -> onResult(result)
                }
            }
            val cancelable = httpClient.unary(finalRequest) httpClientUnary@{ httpResponse ->
                val finalResponse: HTTPResponse
                try {
                    finalResponse = unaryFunc.responseFunction(httpResponse)
                } catch (ex: Throwable) {
                    val connEx = asConnectException(ex)
                    finalOnResult(
                        ResponseMessage.Failure(
                            connEx,
                            emptyMap(),
                            connEx.metadata,
                        ),
                    )
                    return@httpClientUnary
                }
                if (finalResponse.cause != null) {
                    finalOnResult(
                        ResponseMessage.Failure(
                            finalResponse.cause,
                            finalResponse.headers,
                            finalResponse.trailers,
                        ),
                    )
                    return@httpClientUnary
                }
                val responseCodec = serializationStrategy.codec(methodSpec.responseClass)
                val responseMessage: Output
                try {
                    responseMessage = responseCodec.deserialize(finalResponse.message)
                } catch (ex: Exception) {
                    finalOnResult(
                        ResponseMessage.Failure(
                            asConnectException(ex, Code.INTERNAL_ERROR),
                            finalResponse.headers,
                            finalResponse.trailers,
                        ),
                    )
                    return@httpClientUnary
                }
                finalOnResult(
                    ResponseMessage.Success(
                        responseMessage,
                        finalResponse.headers,
                        finalResponse.trailers,
                    ),
                )
            }
            if (requestTimeout != null) {
                timeoutRef.store(config.timeoutScheduler.scheduleTimeout(requestTimeout, cancelable))
            }
            return cancelable
        } catch (ex: Exception) {
            val connEx = asConnectException(ex)
            onResult(
                ResponseMessage.Failure(
                    connEx,
                    emptyMap(),
                    connEx.metadata,
                ),
            )
            return { }
        }
    }

    override suspend fun <Input : Any, Output : Any> unary(
        request: Input,
        headers: Headers,
        methodSpec: MethodSpec<Input, Output>,
    ): ResponseMessage<Output> {
        if (config.ioCoroutineContext != null) {
            return withContext(config.ioCoroutineContext) {
                suspendUnary(request, headers, methodSpec)
            }
        }
        return suspendUnary(request, headers, methodSpec)
    }

    private suspend fun <Input : Any, Output : Any> suspendUnary(
        request: Input,
        headers: Headers,
        methodSpec: MethodSpec<Input, Output>,
    ): ResponseMessage<Output> {
        return suspendCancellableCoroutine { continuation ->
            val cancelable = unary(request, headers, methodSpec) { responseMessage ->
                continuation.resume(responseMessage)
            }
            continuation.invokeOnCancellation {
                cancelable()
            }
        }
    }

    override fun <Input : Any, Output : Any> unaryBlocking(
        request: Input,
        headers: Headers,
        methodSpec: MethodSpec<Input, Output>,
    ): UnaryBlockingCall<Output> {
        return UnaryCall { callback ->
            unary(request, headers, methodSpec, callback)
        }
    }

    override suspend fun <Input : Any, Output : Any> serverStream(
        headers: Headers,
        methodSpec: MethodSpec<Input, Output>,
    ): ServerOnlyStreamInterface<Input, Output> {
        val stream = stream(headers, methodSpec)
        return ServerOnlyStream(stream)
    }

    override suspend fun <Input : Any, Output : Any> clientStream(
        headers: Headers,
        methodSpec: MethodSpec<Input, Output>,
    ): ClientOnlyStreamInterface<Input, Output> {
        val stream = stream(headers, methodSpec)
        return ClientOnlyStream(stream)
    }

    override suspend fun <Input : Any, Output : Any> stream(
        headers: Headers,
        methodSpec: MethodSpec<Input, Output>,
    ): BidirectionalStreamInterface<Input, Output> {
        val channel = Channel<Output>(1)
        val responseHeaders = CompletableDeferred<Headers>()
        val responseTrailers = CompletableDeferred<Headers>()
        val requestCodec = config.serializationStrategy.codec(methodSpec.requestClass)
        val responseCodec = config.serializationStrategy.codec(methodSpec.responseClass)
        val requestTimeout = config.timeoutOracle(methodSpec)
        val request = HTTPRequest(
            url = urlFromMethodSpec(methodSpec),
            contentType = "application/connect+${requestCodec.encodingName()}",
            timeout = requestTimeout,
            headers = headers,
            methodSpec = methodSpec,
        )
        val streamFunc = config.createStreamingInterceptorChain()
        val finalRequest = streamFunc.requestFunction(request)
        val timeoutRef = AtomicReference<Timeout?>(null)
        var isComplete = false
        val httpStream = httpClient.stream(
            request = finalRequest,
            duplex = methodSpec.streamType == StreamType.BIDI,
        ) httpStream@{ initialResult ->
            if (isComplete) {
                // No-op on remaining handlers after a completion.
                return@httpStream
            }
            // Pass through the interceptor chain.
            var streamResult: StreamResult<Buffer>
            try {
                streamResult = streamFunc.streamResultFunction(initialResult)
            } catch (ex: Throwable) {
                streamResult = StreamResult.Complete(asConnectException(ex))
            }
            when (streamResult) {
                is StreamResult.Headers -> {
                    // If this is incorrectly called 2x, only the first result is used.
                    // Subsequent calls to complete will be ignored.
                    responseHeaders.complete(streamResult.headers)
                }

                is StreamResult.Message -> {
                    // Just in case protocol impl failed to provide StreamResult.Headers,
                    // treat headers as empty. This is a no-op if we did correctly receive
                    // them already.
                    responseHeaders.complete(emptyMap())
                    try {
                        val message = responseCodec.deserialize(
                            streamResult.message,
                        )
                        channel.send(message)
                    } catch (ex: Throwable) {
                        isComplete = true
                        var connEx = asConnectException(ex)
                        val timeout = timeoutRef.load()
                        if (timeout != null) {
                            timeout.cancel()
                            if (connEx.code == Code.CANCELED && timeout.timedOut) {
                                connEx = ConnectException(Code.DEADLINE_EXCEEDED, exception = ex)
                            }
                        }
                        try {
                            channel.close(connEx)
                        } finally {
                            responseTrailers.complete(emptyMap())
                        }
                    }
                }

                is StreamResult.Complete -> {
                    // This is a no-op if we already received a StreamResult.Headers.
                    responseHeaders.complete(emptyMap())
                    isComplete = true
                    var connEx = streamResult.cause
                    val timeout = timeoutRef.load()
                    if (timeout != null) {
                        timeout.cancel()
                        if (connEx?.code == Code.CANCELED && timeout.timedOut) {
                            connEx = ConnectException(Code.DEADLINE_EXCEEDED, exception = streamResult.cause)
                        }
                    }
                    try {
                        channel.close(connEx)
                    } finally {
                        responseTrailers.complete(streamResult.trailers)
                    }
                }
            }
        }
        if (requestTimeout != null) {
            timeoutRef.store(
                config.timeoutScheduler.scheduleTimeout(requestTimeout) {
                    runBlocking {
                        channel.close(ConnectException(code = Code.DEADLINE_EXCEEDED, message = "$requestTimeout timeout elapsed"))
                    }
                },
            )
        }
        try {
            channel.invokeOnClose {
                runBlocking { httpStream.receiveClose() }
            }
            var stream = httpStream.transform { streamFunc.requestBodyFunction(it) }
            if (config.ioCoroutineContext != null) {
                stream = stream.dispatchIn(config.ioCoroutineContext)
            }
            return BidirectionalStream(
                stream,
                requestCodec,
                channel,
                responseHeaders,
                responseTrailers,
            )
        } catch (ex: Throwable) {
            // If something in these last steps prevents us
            // from returning, don't leak the stream.
            httpStream.receiveClose()
            throw ex
        }
    }

    private fun urlFromMethodSpec(methodSpec: MethodSpec<*, *>): Url = URLBuilder(baseUrlWithTrailingSlash).appendPathSegments(methodSpec.path).build()
}

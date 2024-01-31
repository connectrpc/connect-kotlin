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
import com.connectrpc.http.Cancelable
import com.connectrpc.http.HTTPClientInterface
import com.connectrpc.http.HTTPRequest
import com.connectrpc.http.transform
import com.connectrpc.protocols.GETConfiguration
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.suspendCancellableCoroutine
import java.net.URI
import java.util.concurrent.CountDownLatch
import kotlin.coroutines.resume

/**
 * Concrete implementation of the [ProtocolClientInterface].
 */
class ProtocolClient(
    // The client to use for performing requests.
    private val httpClient: HTTPClientInterface,
    // The configuration for the ProtocolClient.
    private val config: ProtocolClientConfig,
) : ProtocolClientInterface {

    private val baseURIWithTrailingSlash = if (config.baseUri.path != null && config.baseUri.path.endsWith('/')) {
        config.baseUri
    } else {
        val path = config.baseUri.path ?: ""
        URI(
            config.baseUri.scheme,
            config.baseUri.userInfo,
            config.baseUri.host,
            config.baseUri.port,
            "$path/",
            config.baseUri.query,
            config.baseUri.fragment,
        )
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
            val unaryRequest = HTTPRequest(
                url = urlFromMethodSpec(methodSpec),
                contentType = "application/${requestCodec.encodingName()}",
                headers = headers,
                message = requestMessage.readByteArray(),
                methodSpec = methodSpec,
            )
            val unaryFunc = config.createInterceptorChain()
            val finalRequest = unaryFunc.requestFunction(unaryRequest)
            val cancelable = httpClient.unary(finalRequest) httpClientUnary@{ httpResponse ->
                val finalResponse = unaryFunc.responseFunction(httpResponse)
                val code = finalResponse.code
                val exception = finalResponse.cause?.setErrorParser(serializationStrategy.errorDetailParser())
                if (exception != null) {
                    onResult(
                        ResponseMessage.Failure(
                            exception,
                            code,
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
                } catch (e: Exception) {
                    onResult(
                        ResponseMessage.Failure(
                            ConnectException(code = Code.INTERNAL_ERROR, exception = e),
                            Code.INTERNAL_ERROR,
                            finalResponse.headers,
                            finalResponse.trailers,
                        ),
                    )
                    return@httpClientUnary
                }
                onResult(
                    ResponseMessage.Success(
                        responseMessage,
                        code,
                        finalResponse.headers,
                        finalResponse.trailers,
                    ),
                )
            }
            return cancelable
        } catch (e: Exception) {
            throw e
        }
    }

    override suspend fun <Input : Any, Output : Any> unary(
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
        val countDownLatch = CountDownLatch(1)
        val call = UnaryBlockingCall<Output>()
        // Set the unary synchronous executable.
        call.setExecute { callback: (ResponseMessage<Output>) -> Unit ->
            val cancellationFn = unary(request, headers, methodSpec) { responseMessage ->
                callback(responseMessage)
                countDownLatch.countDown()
            }
            // Set the cancellation function .
            call.setCancel(cancellationFn)
        }
        return call
    }

    override suspend fun <Input : Any, Output : Any> stream(
        headers: Headers,
        methodSpec: MethodSpec<Input, Output>,
    ): BidirectionalStreamInterface<Input, Output> {
        return bidirectionalStream(methodSpec, headers)
    }

    override suspend fun <Input : Any, Output : Any> serverStream(
        headers: Headers,
        methodSpec: MethodSpec<Input, Output>,
    ): ServerOnlyStreamInterface<Input, Output> {
        val stream = bidirectionalStream(methodSpec, headers)
        return ServerOnlyStream(stream)
    }

    override suspend fun <Input : Any, Output : Any> clientStream(
        headers: Headers,
        methodSpec: MethodSpec<Input, Output>,
    ): ClientOnlyStreamInterface<Input, Output> {
        val stream = stream(headers, methodSpec)
        return ClientOnlyStream(stream)
    }

    private suspend fun <Input : Any, Output : Any> bidirectionalStream(
        methodSpec: MethodSpec<Input, Output>,
        headers: Headers,
    ): BidirectionalStream<Input, Output> = suspendCancellableCoroutine { continuation ->
        val channel = Channel<Output>(1)
        val responseHeaders = CompletableDeferred<Headers>()
        val responseTrailers = CompletableDeferred<Headers>()
        val requestCodec = config.serializationStrategy.codec(methodSpec.requestClass)
        val responseCodec = config.serializationStrategy.codec(methodSpec.responseClass)
        val request = HTTPRequest(
            url = urlFromMethodSpec(methodSpec),
            contentType = "application/connect+${requestCodec.encodingName()}",
            headers = headers,
            methodSpec = methodSpec,
        )
        val streamFunc = config.createStreamingInterceptorChain()
        val finalRequest = streamFunc.requestFunction(request)
        var isComplete = false
        val httpStream = httpClient.stream(finalRequest, methodSpec.streamType == StreamType.BIDI) { initialResult ->
            if (isComplete) {
                // No-op on remaining handlers after a completion.
                return@stream
            }
            // Pass through the interceptor chain.
            when (val streamResult = streamFunc.streamResultFunction(initialResult)) {
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
                    } catch (e: Throwable) {
                        isComplete = true
                        try {
                            channel.close(ConnectException(Code.UNKNOWN, exception = e))
                        } finally {
                            responseTrailers.complete(emptyMap())
                        }
                    }
                }

                is StreamResult.Complete -> {
                    // This is a no-op if we already received a StreamResult.Headers.
                    responseHeaders.complete(emptyMap())
                    isComplete = true
                    try {
                        when (streamResult.code) {
                            Code.OK -> channel.close()
                            else -> channel.close(streamResult.connectException() ?: ConnectException(code = streamResult.code, exception = streamResult.cause))
                        }
                    } finally {
                        responseTrailers.complete(streamResult.trailers)
                    }
                }
            }
        }
        continuation.invokeOnCancellation {
            httpStream.receiveClose()
        }
        val stream = httpStream.transform { streamFunc.requestBodyFunction(it) }
        channel.invokeOnClose {
            stream.receiveClose()
        }
        continuation.resume(
            BidirectionalStream(
                stream,
                requestCodec,
                channel,
                responseHeaders,
                responseTrailers,
            ),
        )
    }

    private fun urlFromMethodSpec(methodSpec: MethodSpec<*, *>) = baseURIWithTrailingSlash.resolve(methodSpec.path).toURL()
}

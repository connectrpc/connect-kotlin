// Copyright 2022-2023 Buf Technologies, Inc.
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

package build.buf.connect.impl

import build.buf.connect.BidirectionalStreamInterface
import build.buf.connect.ClientOnlyStreamInterface
import build.buf.connect.Code
import build.buf.connect.Headers
import build.buf.connect.MethodSpec
import build.buf.connect.ProtocolClientConfig
import build.buf.connect.ProtocolClientInterface
import build.buf.connect.ResponseMessage
import build.buf.connect.ServerOnlyStreamInterface
import build.buf.connect.StreamResult
import build.buf.connect.UnaryBlockingCall
import build.buf.connect.http.Cancelable
import build.buf.connect.http.HTTPClientInterface
import build.buf.connect.http.HTTPRequest
import build.buf.connect.http.Stream
import build.buf.connect.protocols.GETConfiguration
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.suspendCancellableCoroutine
import java.net.URL
import java.util.concurrent.CountDownLatch
import kotlin.coroutines.resume

/**
 * Concrete implementation of the [ProtocolClientInterface].
 */
class ProtocolClient(
    // The client to use for performing requests.
    private val httpClient: HTTPClientInterface,
    // The configuration for the ProtocolClient.
    private val config: ProtocolClientConfig
) : ProtocolClientInterface {

    override fun <Input : Any, Output : Any> unary(
        request: Input,
        headers: Headers,
        methodSpec: MethodSpec<Input, Output>,
        onResult: (ResponseMessage<Output>) -> Unit
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
                methodSpec = methodSpec
            )
            val unaryFunc = config.createInterceptorChain()
            val finalRequest = unaryFunc.requestFunction(unaryRequest)
            val cancelable = httpClient.unary(finalRequest) { httpResponse ->
                val finalResponse = unaryFunc.responseFunction(httpResponse)
                val code = finalResponse.code
                val connectError = finalResponse.error?.setErrorParser(serializationStrategy.errorDetailParser())
                if (connectError != null) {
                    onResult(
                        ResponseMessage.Failure(
                            connectError,
                            code,
                            finalResponse.headers,
                            finalResponse.trailers
                        )
                    )
                } else {
                    val responseCodec = serializationStrategy.codec(methodSpec.responseClass)
                    val responseMessage = responseCodec.deserialize(
                        finalResponse.message
                    )
                    onResult(
                        ResponseMessage.Success(
                            responseMessage,
                            code,
                            finalResponse.headers,
                            finalResponse.trailers
                        )
                    )
                }
            }
            return cancelable
        } catch (e: Exception) {
            throw e
        }
    }

    override suspend fun <Input : Any, Output : Any> unary(
        request: Input,
        headers: Headers,
        methodSpec: MethodSpec<Input, Output>
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
        methodSpec: MethodSpec<Input, Output>
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
        methodSpec: MethodSpec<Input, Output>
    ): BidirectionalStreamInterface<Input, Output> {
        return bidirectionalStream(methodSpec, headers)
    }

    override suspend fun <Input : Any, Output : Any> serverStream(
        headers: Headers,
        methodSpec: MethodSpec<Input, Output>
    ): ServerOnlyStreamInterface<Input, Output> {
        val stream = stream(headers, methodSpec)
        return ServerOnlyStream(stream)
    }

    override suspend fun <Input : Any, Output : Any> clientStream(
        headers: Headers,
        methodSpec: MethodSpec<Input, Output>
    ): ClientOnlyStreamInterface<Input, Output> {
        val stream = stream(headers, methodSpec)
        return ClientOnlyStream(stream)
    }

    private suspend fun <Input : Any, Output : Any> bidirectionalStream(
        methodSpec: MethodSpec<Input, Output>,
        headers: Headers
    ): BidirectionalStreamInterface<Input, Output> = suspendCancellableCoroutine { continuation ->
        val channel = Channel<StreamResult<Output>>(1)
        val requestCodec = config.serializationStrategy.codec(methodSpec.requestClass)
        val responseCodec = config.serializationStrategy.codec(methodSpec.responseClass)
        val request = HTTPRequest(
            url = urlFromMethodSpec(methodSpec),
            contentType = "application/connect+${requestCodec.encodingName()}",
            headers = headers,
            methodSpec = methodSpec
        )
        val streamFunc = config.createStreamingInterceptorChain()
        val finalRequest = streamFunc.requestFunction(request)
        var isComplete = false
        val httpStream = httpClient.stream(finalRequest) { initialResult ->
            if (isComplete) {
                // No-op on remaining handlers after a completion.
                return@stream
            }
            // Pass through the interceptor chain.
            val streamResult = streamFunc.streamResultFunction(initialResult)
            val result: StreamResult<Output> = when (streamResult) {
                is StreamResult.Headers -> {
                    StreamResult.Headers(streamResult.headers)
                }

                is StreamResult.Message -> {
                    try {
                        val message = responseCodec.deserialize(
                            streamResult.message
                        )
                        StreamResult.Message(message)
                    } catch (e: Throwable) {
                        isComplete = true
                        StreamResult.Complete(Code.UNKNOWN, e)
                    }
                }

                is StreamResult.Complete -> {
                    isComplete = true
                    StreamResult.Complete(
                        streamResult.connectError()?.code ?: Code.OK,
                        error = streamResult.error,
                        trailers = streamResult.trailers
                    )
                }
            }
            channel.send(result)
        }
        continuation.invokeOnCancellation {
            httpStream.close()
        }
        continuation.resume(
            BidirectionalStream(
                Stream(
                    onSend = { buffer ->
                        httpStream.send(streamFunc.requestBodyFunction(buffer))
                    },
                    onClose = {
                        httpStream.close()
                    }
                ),
                requestCodec,
                channel
            )
        )
    }

    private fun <Input : Any, Output : Any> urlFromMethodSpec(methodSpec: MethodSpec<Input, Output>): URL {
        val host = config.baseUri.resolve("/${methodSpec.path}")
        return host.toURL()
    }
}

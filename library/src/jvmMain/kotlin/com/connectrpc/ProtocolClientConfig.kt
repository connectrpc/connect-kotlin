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

package com.connectrpc

import com.connectrpc.compression.CompressionPool
import com.connectrpc.compression.GzipCompressionPool
import com.connectrpc.http.Timeout
import com.connectrpc.protocols.ConnectInterceptor
import com.connectrpc.protocols.GETConfiguration
import com.connectrpc.protocols.GRPCInterceptor
import com.connectrpc.protocols.GRPCWebInterceptor
import com.connectrpc.protocols.NetworkProtocol
import io.ktor.http.Url
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

typealias TimeoutOracle = (MethodSpec<*, *>) -> Duration?

/**
 * Returns an oracle that provides the given timeouts for unary or stream
 * operations, respectively.
 */
fun simpleTimeouts(unaryTimeout: Duration?, streamTimeout: Duration?): TimeoutOracle {
    return { methodSpec ->
        when (methodSpec.streamType) {
            StreamType.UNARY -> unaryTimeout
            else -> streamTimeout
        }
    }
}

/**
 *  Set of configuration used to set up clients.
 */
class ProtocolClientConfig @JvmOverloads constructor(
    // TODO: Use a block-based construction pattern instead of JvmOverloads
    //       so we can add new fields in the future without having to worry
    //       about their ordering or potentially breaking compatibility with
    //       already-compiled byte code.

    // The host (e.g., https://connectrpc.com).
    val host: String,
    // The client to use for performing requests.
    // The serialization strategy for decoding messages.
    val serializationStrategy: SerializationStrategy,
    // The protocol to use.
    networkProtocol: NetworkProtocol = NetworkProtocol.CONNECT,
    // The compression type that should be used (e.g., "gzip").
    // Defaults to no compression.
    val requestCompression: RequestCompression? = null,
    // The GET configuration for the Connect protocol.
    // By default, this is disabled.
    val getConfiguration: GETConfiguration = GETConfiguration.Disabled,
    // Set of interceptors that should be invoked with requests/responses.
    interceptors: List<(ProtocolClientConfig) -> Interceptor> = emptyList(),
    // Compression pools that provide support for the provided `compressionName`, as well as any
    // other compression methods that need to be supported for inbound responses.
    compressionPools: List<CompressionPool> = listOf(GzipCompressionPool),
    // The coroutine context to use for I/O, such as sending RPC messages.
    // If null, the current/calling coroutine context is used. So the caller
    // may need to explicitly dispatch send calls using contexts where I/O
    // is appropriate (using the withContext extension function). If non-null
    // (such as Dispatchers.IO), operations that involve I/O or other
    // blocking will automatically be dispatched using the given context,
    // so the caller does not need to worry about it.
    val ioCoroutineContext: CoroutineContext? = null,
    // A function that is consulted to determine timeouts for each RPC. If
    // the function returns null, no timeout is applied. If a non-null value
    // is returned, the entire call must complete before it elapses. If the
    // call is still active at the end of the timeout period, it is cancelled
    // and will result in an exception with a Code.DEADLINE_EXCEEDED code.
    //
    // The default oracle, if not configured, returns a 10 second timeout for
    // all operations.
    val timeoutOracle: TimeoutOracle = { 10.toDuration(DurationUnit.SECONDS) },
    // Schedules timeout actions.
    val timeoutScheduler: Timeout.Scheduler = Timeout.DEFAULT_SCHEDULER,
) {
    private val internalInterceptorFactoryList = mutableListOf<(ProtocolClientConfig) -> Interceptor>()
    private val compressionPools = mutableMapOf<String, CompressionPool>()
    internal val baseUrl: Url

    init {
        val protocolInterceptor: (ProtocolClientConfig) -> Interceptor = when (networkProtocol) {
            NetworkProtocol.CONNECT -> { params ->
                ConnectInterceptor(params)
            }

            NetworkProtocol.GRPC -> { params ->
                GRPCInterceptor(params)
            }

            NetworkProtocol.GRPC_WEB -> { params ->
                GRPCWebInterceptor(params)
            }
        }
        internalInterceptorFactoryList.addAll(interceptors)
        // The protocol interceptor is registered last.
        // It would be the last outbound filter and the first inbound filter.
        // This would allow users to have confidence in modifying the request before the protocol
        // interceptor and would allow for modifying response after the protocol interceptor.
        internalInterceptorFactoryList.add(protocolInterceptor)
        for (compressionPool in compressionPools) {
            this.compressionPools[compressionPool.name()] = compressionPool
        }
        baseUrl = Url(host)
        val scheme = baseUrl.protocol.name
        require(scheme == "http" || scheme == "https") {
            "Unsupported URL scheme: $scheme (only http and https are supported)"
        }
    }

    /**
     * Get the compression pool by name.
     *
     * @param name The name of the compression pool.
     */
    fun compressionPool(name: String?): CompressionPool? {
        return compressionPools[name]
    }

    /**
     * Get the registered compression pools for the configuration.
     *
     * @return The list of registered compression pools.
     */
    fun compressionPools(): List<CompressionPool> {
        return compressionPools.map { entry -> entry.value }
    }

    /**
     * Creates an interceptor chain from the list of interceptors for unary based requests.
     */
    fun createInterceptorChain(): UnaryFunction {
        val finalInterceptor = chain(internalInterceptorFactoryList)
        return finalInterceptor.unaryFunction()
    }

    /**
     * Creates an interceptor chain from the list of interceptors for streaming based requests.
     */
    fun createStreamingInterceptorChain(): StreamFunction {
        val finalInterceptor = chain(internalInterceptorFactoryList)
        return finalInterceptor.streamFunction()
    }

    private fun chain(
        interceptorFactories: List<(ProtocolClientConfig) -> Interceptor>,
    ): Interceptor {
        val interceptors = interceptorFactories.map { factory -> factory(this) }
        return object : Interceptor {
            override fun unaryFunction(): UnaryFunction {
                val unaryFunctions = interceptors.map { interceptor -> interceptor.unaryFunction() }
                return UnaryFunction(
                    requestFunction = { httpRequest ->
                        var request = httpRequest
                        for (unaryFunction in unaryFunctions) {
                            request = unaryFunction.requestFunction(request)
                        }
                        request
                    },
                    responseFunction = { httpResponse ->
                        var response = httpResponse
                        for (unaryFunction in unaryFunctions.reversed()) {
                            response = unaryFunction.responseFunction(response)
                        }
                        response
                    },
                )
            }

            override fun streamFunction(): StreamFunction {
                val streamFunctions = interceptors.map { interceptor -> interceptor.streamFunction() }
                return StreamFunction(
                    requestFunction = { httpRequest ->
                        var request = httpRequest
                        for (streamFunction in streamFunctions) {
                            request = streamFunction.requestFunction(request)
                        }
                        request
                    },
                    requestBodyFunction = { requestBody ->
                        var body = requestBody
                        for (streamFunction in streamFunctions) {
                            body = streamFunction.requestBodyFunction(body)
                        }
                        body
                    },
                    streamResultFunction = { streamResult ->
                        var result = streamResult
                        for (streamFunction in streamFunctions.reversed()) {
                            result = streamFunction.streamResultFunction(result)
                        }
                        result
                    },
                )
            }
        }
    }
}

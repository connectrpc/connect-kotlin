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

package build.buf.connect

import build.buf.connect.compression.CompressionPool
import build.buf.connect.compression.GzipCompressionPool
import build.buf.connect.compression.RequestCompression
import build.buf.connect.protocols.ConnectInterceptor
import build.buf.connect.protocols.GRPCInterceptor
import build.buf.connect.protocols.GRPCWebInterceptor
import build.buf.connect.protocols.NetworkProtocol
import java.net.URI

/**
 *  Set of configuration used to set up clients.
 */
class ProtocolClientConfig(
    // The host (e.g., https://buf.build).
    val host: String,
    // The client to use for performing requests.
    // The serialization strategy for decoding messages.
    val serializationStrategy: SerializationStrategy,
    // The protocol to use.
    networkProtocol: NetworkProtocol = NetworkProtocol.CONNECT,
    // The compression type that should be used (e.g., "gzip").
    // Defaults to no compression.
    val requestCompression: RequestCompression? = null,
    val enableGet: Boolean = false,
    val getMaxUrlBytes: Int = 50_000,
    val getFallback: Boolean = true,
    // Set of interceptors that should be invoked with requests/responses.
    interceptors: List<(ProtocolClientConfig) -> Interceptor> = emptyList(),
    // Compression pools that provide support for the provided `compressionName`, as well as any
    // other compression methods that need to be supported for inbound responses.
    compressionPools: List<CompressionPool> = listOf(GzipCompressionPool)
) {
    private val internalInterceptorFactoryList = mutableListOf<(ProtocolClientConfig) -> Interceptor>()
    private val compressionPools = mutableMapOf<String, CompressionPool>()
    internal val baseUri: URI

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
            this.compressionPools.put(compressionPool.name(), compressionPool)
        }
        baseUri = URI(host)
    }

    /**
     * Get the compression pool by name.
     *
     * @param name The name of the compression pool.
     */
    fun compressionPool(name: String?): CompressionPool? {
        if (compressionPools.containsKey(name)) {
            return compressionPools[name]!!
        }
        return null
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
        interceptorFactories: List<(ProtocolClientConfig) -> Interceptor>
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
                    }
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
                    }
                )
            }
        }
    }
}

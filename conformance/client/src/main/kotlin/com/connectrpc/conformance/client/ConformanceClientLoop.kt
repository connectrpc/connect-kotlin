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

import com.connectrpc.conformance.client.adapt.ClientCompatRequest
import com.connectrpc.conformance.client.adapt.ClientCompatResponse
import kotlinx.coroutines.runBlocking
import java.io.EOFException
import java.io.InputStream
import java.io.OutputStream

/**
 * The main loop that a conformance client program executes. This
 * loop reads requests from stdin, uses a Client to issue the RPC
 * described by the request, and writes the results of the RPC to
 * stdout.
 */
class ConformanceClientLoop(
    private val requestUnmarshaller: (ByteArray) -> ClientCompatRequest,
    private val responseMarshaller: (ClientCompatResponse) -> ByteArray,
    private val verbose: VerbosePrinter,
) {
    fun run(input: InputStream, output: OutputStream, client: Client) = runBlocking {
        // TODO: issue RPCs in parallel
        while (true) {
            var result: ClientCompatResponse.Result
            val req = readRequest(input) ?: return@runBlocking // end of stream
            verbose.verbosity(1) {
                println("read request for test ${req.testName}")
                verbose.verbosity(3) {
                    println("RPC request:")
                    indent().println("${req.raw}")
                }
            }
            try {
                val resp = client.handle(req)
                result = ClientCompatResponse.Result.ResponseResult(resp)
                verbose.verbosity(1) {
                    println("RPC completed for test ${req.testName}")
                }
            } catch (ex: Exception) {
                verbose.verbosity(1) {
                    println("RPC could not be issued for test ${req.testName}")
                    indent().println(ex.stackTraceToString())
                }
                val msg = if (ex.message.orEmpty() == "") {
                    ex::class.qualifiedName.orEmpty()
                } else {
                    "${ex::class.qualifiedName}: ${ex.message}"
                }
                result = ClientCompatResponse.Result.ErrorResult(msg)
            }
            if (result is ClientCompatResponse.Result.ResponseResult && result.response.error != null) {
                verbose.verbosity(2) {
                    val ex = result.response.error!!
                    println("RPC failed with code ${ex.code} for test ${req.testName}:")
                    indent().println(ex.stackTraceToString())
                }
            }
            writeResponse(
                output,
                ClientCompatResponse(
                    testName = req.testName,
                    result = result,
                ),
            )
            if (result is ClientCompatResponse.Result.ResponseResult && result.response.raw != null) {
                verbose.verbosity(3) {
                    println("RPC result:")
                    indent().println("${result.response.raw}")
                }
            }
        }
    }

    private fun readRequest(input: InputStream): ClientCompatRequest? {
        val len = input.readInt() ?: return null
        val data = input.readN(len)
            ?: throw EOFException("unexpected EOF: read 0 of $len expected message bytes")
        return requestUnmarshaller(data)
    }

    private fun writeResponse(output: OutputStream, resp: ClientCompatResponse) {
        val respBytes = responseMarshaller(resp)
        val prefix = ByteArray(4)
        val len = respBytes.size
        prefix[0] = len.ushr(24).toByte()
        prefix[1] = len.ushr(16).toByte()
        prefix[2] = len.ushr(8).toByte()
        prefix[3] = len.toByte()
        output.write(prefix)
        output.write(respBytes)
    }

    private fun InputStream.readN(len: Int): ByteArray? {
        val bytes = ByteArray(len)
        var offs = 0
        var remain = len
        while (remain > 0) {
            val n = this.read(bytes, offs, remain)
            when (n) {
                -1, 0 -> {
                    if (offs == 0) {
                        return null
                    }
                    throw EOFException("unexpected EOF: read $offs of $len expected bytes")
                }

                else -> {
                    offs += n
                    remain -= n
                }
            }
        }
        return bytes
    }

    private fun InputStream.readInt(): Int? {
        val bytes = this.readN(4) ?: return null
        return bytes[0].toInt().and(0xff).shl(24) or
            bytes[1].toInt().and(0xff).shl(16) or
            bytes[2].toInt().and(0xff).shl(8) or
            bytes[3].toInt().and(0xff)
    }
}

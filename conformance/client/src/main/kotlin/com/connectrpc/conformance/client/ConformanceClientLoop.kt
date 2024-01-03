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

import com.connectrpc.conformance.client.adapt.UnaryClient.InvokeStyle
import com.connectrpc.lite.connectrpc.conformance.v1.ClientCompatRequest
import com.connectrpc.lite.connectrpc.conformance.v1.ClientCompatResponse
import com.connectrpc.lite.connectrpc.conformance.v1.ClientErrorResult
import com.google.protobuf.ByteString
import kotlinx.coroutines.runBlocking
import java.io.EOFException
import java.io.InputStream
import java.io.OutputStream

class ConformanceClientLoop {
    companion object {
        fun run(input: InputStream, output: OutputStream, client: Client) = runBlocking {
            // TODO: issue RPCs in parallel
            while (true) {
                var result: ClientCompatResponse
                val req = readRequest(input) ?: return@runBlocking // end of stream
                try {
                    val resp = client.handle(req)
                    result = ClientCompatResponse.newBuilder().setResponse(resp).build()
                } catch (e: Exception) {
                    val msg = if (e.message.orEmpty() == "") {
                        e::class.qualifiedName
                    } else {
                        "${e::class.qualifiedName}: ${e.message}"
                    }
                    result = ClientCompatResponse.newBuilder()
                        .setError(ClientErrorResult.newBuilder().setMessage(msg))
                        .build()
                }
                writeResponse(output, result)
            }
        }

        private fun readRequest(input: InputStream): ClientCompatRequest? {
            val len = input.readInt() ?: return null
            val data = input.readN(len)
                ?: throw EOFException("unexpected EOF: read 0 of $len expected message bytes")
            return ClientCompatRequest.parseFrom(ByteString.copyFrom(data))
        }

        private fun writeResponse(output: OutputStream, resp: ClientCompatResponse) {
            val len = resp.serializedSize
            val prefix = ByteArray(4)
            prefix[0] = len.ushr(24).toByte()
            prefix[1] = len.ushr(16).toByte()
            prefix[2] = len.ushr(8).toByte()
            prefix[3] = len.toByte()
            output.write(prefix)
            resp.writeTo(output)
        }

        private fun InputStream.readN(len: Int): ByteArray? {
            val bytes = ByteArray(len)
            var offs = 0
            var remain = len
            while (remain > 0) {
                val n = this.read(bytes, offs, remain)
                if (n == 0) {
                    if (offs == 0) {
                        return null
                    }
                    throw EOFException("unexpected EOF: read $offs of $len expected bytes")
                }
                offs += n
                remain -= n
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

        fun parseArgs(args: Array<String>): InvokeStyle {
            if (args.isEmpty()) {
                return InvokeStyle.SUSPEND
            }
            if (args.size > 1) {
                throw IllegalArgumentException("expecting exactly one args (invoke style), but got ${args.size}")
            }
            return when (args[0].lowercase()) {
                "suspend" -> InvokeStyle.SUSPEND
                "blocking" -> InvokeStyle.BLOCKING
                "callback" -> InvokeStyle.CALLBACK
                else -> throw IllegalArgumentException("expecting one args to be 'suspend', 'blocking', or 'callback', but got '${args[0]}'")
            }
        }
    }
}

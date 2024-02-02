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

import com.connectrpc.StreamResult
import com.connectrpc.http.Cancelable
import com.connectrpc.http.HTTPClientInterface
import com.connectrpc.http.HTTPRequest
import com.connectrpc.http.HTTPResponse
import com.connectrpc.http.Stream
import com.connectrpc.http.UnaryHTTPRequest
import com.connectrpc.http.clone
import okio.Buffer

internal class TracingHTTPClient(
    private val delegate: HTTPClientInterface,
    private val printer: VerbosePrinter.Printer,
) : HTTPClientInterface {
    override fun unary(request: UnaryHTTPRequest, onResult: (HTTPResponse) -> Unit): Cancelable {
        printer.printlnWithStackTrace("Sending unary request (${request.message.size} bytes): ${request.httpMethod} ${request.url}")
        val cancel = delegate.unary(request) { response ->
            val buffer = Buffer()
            buffer.writeAll(response.message)
            if (response.cause != null) {
                printer.println("Failed to receive HTTP response (${buffer.size} bytes): ${response.cause!!.message.orEmpty()}")
                printer.indent().println(response.cause!!.stackTraceToString())
            } else {
                printer.println("Received HTTP response (${buffer.size} bytes): ${response.status ?: "???"}")
            }
            onResult(response.clone(message = buffer))
        }
        return {
            printer.println("Canceling HTTP request...")
            cancel()
        }
    }

    override fun stream(
        request: HTTPRequest,
        duplex: Boolean,
        onResult: suspend (StreamResult<Buffer>) -> Unit,
    ): Stream {
        printer.printlnWithStackTrace("Sending HTTP stream request: POST ${request.url}")
        val stream = delegate.stream(request, duplex) { result ->
            when (result) {
                is StreamResult.Headers -> {
                    printer.printlnWithStackTrace("Received HTTP response headers")
                }
                is StreamResult.Message -> {
                    printer.printlnWithStackTrace("Received HTTP response data (${result.message.size} bytes)")
                }
                is StreamResult.Complete -> {
                    if (result.cause != null) {
                        printer.printlnWithStackTrace("Failed to complete HTTP response (code=${result.cause!!.code}): ${result.cause!!.message.orEmpty()}")
                    } else {
                        printer.printlnWithStackTrace("Received successful HTTP response completion")
                    }
                }
            }
            onResult(result)
        }
        return TracingStream(stream, printer)
    }

    private class TracingStream(
        private val delegate: Stream,
        private val printer: VerbosePrinter.Printer,
    ) : Stream {
        override suspend fun send(buffer: Buffer): Result<Unit> {
            val size = buffer.size
            val res = delegate.send(buffer)
            if (res.isFailure) {
                printer.printlnWithStackTrace("Failed to send HTTP request data ($size bytes): ${res.exceptionOrNull()!!.message}")
            } else {
                printer.printlnWithStackTrace("Sent HTTP request data ($size bytes)")
            }
            return res
        }

        override suspend fun sendClose() {
            printer.printlnWithStackTrace("Half-closing stream")
            delegate.sendClose()
        }

        override suspend fun receiveClose() {
            printer.printlnWithStackTrace("Closing stream")
            delegate.receiveClose()
        }

        override fun isSendClosed(): Boolean {
            return delegate.isSendClosed()
        }

        override fun isReceiveClosed(): Boolean {
            return delegate.isReceiveClosed()
        }
    }
}

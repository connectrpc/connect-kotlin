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

import com.connectrpc.okhttp.originalCode
import okhttp3.Call
import okhttp3.Connection
import okhttp3.EventListener
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Proxy

internal class OkHttpEventTracer(
    private val printer: VerbosePrinter.Printer,
) : EventListener() {
    override fun connectStart(call: Call, inetSocketAddress: InetSocketAddress, proxy: Proxy) {
        printer.printlnWithStackTrace("connecting to $inetSocketAddress...")
    }
    override fun connectEnd(
        call: Call,
        inetSocketAddress: InetSocketAddress,
        proxy: Proxy,
        protocol: Protocol?,
    ) {
        printer.printlnWithStackTrace("connected to $inetSocketAddress")
    }
    override fun connectFailed(
        call: Call,
        inetSocketAddress: InetSocketAddress,
        proxy: Proxy,
        protocol: Protocol?,
        ioe: IOException,
    ) {
        printer.printlnWithStackTrace("connect to $inetSocketAddress failed")
    }
    override fun connectionAcquired(call: Call, connection: Connection) {
        printer.printlnWithStackTrace("connection to ${connection.socket().remoteSocketAddress} acquired")
    }
    override fun requestHeadersStart(call: Call) {
        printer.printlnWithStackTrace("writing request headers...")
    }
    override fun requestHeadersEnd(call: Call, request: Request) {
        printer.printlnWithStackTrace("request headers written")
    }
    override fun requestBodyStart(call: Call) {
        printer.printlnWithStackTrace("writing request body...")
    }
    override fun requestBodyEnd(call: Call, byteCount: Long) {
        printer.printlnWithStackTrace("request body written: $byteCount bytes")
    }
    override fun requestFailed(call: Call, ioe: IOException) {
        printer.printlnWithStackTrace("request failed: ${ioe.message}")
    }
    override fun responseHeadersStart(call: Call) {
        printer.printlnWithStackTrace("reading response headers...")
    }
    override fun responseHeadersEnd(call: Call, response: Response) {
        printer.printlnWithStackTrace("response headers read: status code = ${response.originalCode()}")
    }
    override fun responseBodyStart(call: Call) {
        printer.printlnWithStackTrace("reading response body...")
    }
    override fun responseBodyEnd(call: Call, byteCount: Long) {
        printer.printlnWithStackTrace("response body read: $byteCount bytes")
    }
    override fun responseFailed(call: Call, ioe: IOException) {
        printer.printlnWithStackTrace("response failed: ${ioe.message}")
    }
}

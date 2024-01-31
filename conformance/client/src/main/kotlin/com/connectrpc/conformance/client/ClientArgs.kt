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

data class ClientArgs(
    val invokeStyle: InvokeStyle,
    val verbose: VerbosePrinter,
) {
    companion object {
        fun parseArgs(args: Array<String>): ClientArgs {
            var invokeStyle = InvokeStyle.SUSPEND
            var verbosity = 0
            var skip = false
            for (i in args.indices) {
                if (skip) {
                    skip = false
                    continue
                }
                when (val arg = args[i]) {
                    "-s", "--style" -> {
                        if (i == args.size - 1) {
                            throw RuntimeException("$arg option requires a value")
                        }
                        skip = true // consuming next string now
                        val v = args[i + 1]
                        when (v.lowercase()) {
                            "suspend" -> {
                                invokeStyle = InvokeStyle.SUSPEND
                            }
                            "callback" -> {
                                invokeStyle = InvokeStyle.CALLBACK
                            }
                            "blocking" -> {
                                invokeStyle = InvokeStyle.BLOCKING
                            }
                            else -> {
                                throw RuntimeException("value for $arg option should be 'suspend', 'callback', or 'blocking'; instead got '$v'")
                            }
                        }
                    }
                    "-v" -> {
                        if (i == args.size - 1) {
                            throw RuntimeException("$arg option requires a value")
                        }
                        skip = true // consuming next string now
                        val v = args[i + 1]
                        val intVal = v.toIntOrNull()
                        if (intVal == null || intVal < 1 || intVal > 5) {
                            throw RuntimeException("value for $arg option should be an integer between 1 and 5; instead got '$v'")
                        }
                        verbosity = intVal
                    }
                    else -> {
                        if (arg.startsWith("-")) {
                            throw RuntimeException("unknown option: $arg")
                        } else {
                            throw RuntimeException("this command does not accept positional arguments")
                        }
                    }
                }
            }
            return ClientArgs(invokeStyle, VerbosePrinter(verbosity, "* client: "))
        }
    }
}

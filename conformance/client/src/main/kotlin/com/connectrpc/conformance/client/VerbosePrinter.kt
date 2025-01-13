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

/**
 * Helper for printing verbose output. This can be useful
 * for troubleshooting and debugging.
 */
class VerbosePrinter(
    private val verbosity: Int,
    private val prefix: String,
) {
    companion object {
        /**
         * Verbosity required to log optional stack traces.
         */
        private const val STACK_TRACE_VERBOSITY = 5

        /**
         * Lock used to synchronize all output so that concurrent
         * threads printing don't interleave optional stack traces
         * (which makes them much harder to read)
         */
        private val lock = Object()
    }

    private val output = PrinterImpl(verbosity, prefix)

    /**
     * Runs the given block with the given verbosity. If the
     * given verbosity is higher than the currently configured
     * output, the block will not be executed.
     */
    fun verbosity(v: Int, block: Printer.() -> Unit) {
        if (v > verbosity) return
        block(output)
    }

    /**
     * Returns a new printer with the given additional prefix
     * added to each printed line.
     */
    fun withPrefix(prefix: String): VerbosePrinter {
        return VerbosePrinter(verbosity, this.prefix + prefix)
    }

    /**
     * Prints verbose messages.
     */
    interface Printer {
        fun println(s: String)

        /**
         * Like `println` but will also record a stack trace
         * of the caller if verbosity is sufficiently high.
         */
        fun printlnWithStackTrace(s: String)

        /**
         * Returns a printer whose output is prefixed with an
         * extra indentation (tab stop).
         */
        fun indent(): Printer
    }

    private class PrinterImpl(
        private val verbosity: Int,
        private val prefix: String,
    ) : Printer {
        override fun println(s: String) {
            synchronized(lock) {
                s.splitToSequence("\n").forEach {
                    System.err.println("${prefix}$it")
                }
            }
        }
        override fun printlnWithStackTrace(s: String) {
            synchronized(lock) {
                println(s)
                if (verbosity < STACK_TRACE_VERBOSITY) {
                    return
                }
                println(
                    RuntimeException()
                        .stackTraceToString()
                        // Skip first two lines. First one is the exception
                        // type and message. Second one is stack-frame for
                        // this function.
                        .substringAfter("\n")
                        .substringAfter("\n")
                        // Trim trailing newline.
                        .trimEnd('\n'),
                )
            }
        }
        override fun indent(): Printer {
            return PrinterImpl(verbosity, prefix + "\t")
        }
    }
}

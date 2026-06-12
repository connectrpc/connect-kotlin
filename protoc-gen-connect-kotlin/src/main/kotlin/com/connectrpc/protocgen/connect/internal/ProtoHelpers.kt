// Copyright 2022-2026 The Connect Authors
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

package com.connectrpc.protocgen.connect.internal

/*
 * Various helper methods for protoc plugins.
 * Original code from: https://codereview.appspot.com/912042.
 *
 * @author t.broyer@ltgt.net Thomas Broyer
 * Based on the initial work of:
 *  @author kenton@google.com Kenton Varda
 * Based on original Protocol Buffers design by
 *  Sanjay Ghemawat, Jeff Dean, and others.
 */

/**
 * Parses a set of comma-delimited name/value pairs.
 *
 *
 * Several code generators treat the parameter argument as holding a list of
 * options separated by commas: e.g., `"foo=bar,baz,qux=corge"` parses
 * to the pairs: `("foo", "bar"), ("baz", ""), ("qux", "corge")`.
 *
 *
 * When a key is present several times, only the last value is retained.
 */
internal fun parseGeneratorParameter(
    text: String,
): Map<String, String> {
    if (text.isEmpty()) {
        return emptyMap()
    }
    val result: MutableMap<String, String> = HashMap()
    val parts = text.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
    for (part in parts) {
        if (part.isEmpty()) {
            continue
        }
        val equalsPos = part.indexOf('=')
        var key: String
        var value: String
        if (equalsPos < 0) {
            key = part
            value = ""
        } else {
            key = part.substring(0, equalsPos)
            value = part.substring(equalsPos + 1)
        }
        val normalizedKey = underscoresToCamelCaseImpl(key, false)
        result[normalizedKey] = value
    }
    return result
}

/**
 * Converts the given input to camel-case, specifying whether the first
 * letter should be upper-case or lower-case.
 *
 * @param input         string to be converted
 * @param capFirstLetter `true` if the first letter should be turned to
 * upper-case.
 * @return the camel-cased string
 */
private fun underscoresToCamelCaseImpl(
    input: String,
    capFirstLetter: Boolean,
): String = buildString(input.length) {
    var capNextLetter = capFirstLetter
    for ((i, c) in input.withIndex()) {
        capNextLetter = when (c) {
            in 'a'..'z' -> {
                if (capNextLetter) {
                    append(c.uppercaseChar())
                } else {
                    append(c)
                }
                false
            }

            in 'A'..'Z' -> {
                if (i == 0 && !capNextLetter) {
                    // Force first letter to lower-case unless explicitly told
                    // to capitalize it.
                    append(c.lowercaseChar())
                } else {
                    // Capital letters after the first are left as-is.
                    append(c)
                }
                false
            }

            in '0'..'9' -> {
                append(c)
                true
            }

            else -> true
        }
    }
}

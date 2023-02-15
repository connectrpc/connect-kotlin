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

package build.buf.connect.protocols

import build.buf.connect.Code
import build.buf.connect.ConnectErrorDetail
import build.buf.connect.ErrorDetailParser
import build.buf.connect.Trailers
import okio.Buffer
import okio.ByteString
import okio.ByteString.Companion.decodeBase64
import okio.ByteString.Companion.encodeUtf8

class GRPCCompletionParser(
    private val errorDetailParser: ErrorDetailParser
) {
    /**
     * Parses the completion of a GRPC response from the Trailers.
     *
     * For GRPCWeb, the caller will have to transform the final message into trailers to parse a completion.
     *
     * For GRPC H2, the caller can just take the trailers from the completed stream to parse a completion.
     *
     * Returns null when a completion is unable to be parsed.
     */
    internal fun parse(trailers: Trailers): GRPCCompletion? {
        val status = parseStatus(trailers) ?: return null
        val code = Code.fromValue(status)
        val message = parseMessage(trailers)
        val details = connectErrorDetails(trailers)
        return GRPCCompletion(
            code,
            status,
            message,
            details
        )
    }

    private fun connectErrorDetails(trailers: Trailers): List<ConnectErrorDetail> {
        val rawError = trailers[GRPC_STATUS_DETAILS_TRAILERS]?.first()?.decodeBase64()?.toByteArray()
        if (rawError != null) {
            try {
                return errorDetailParser.parseDetails(rawError)
            } catch (_: Throwable) {
                // no-op
            }
        }
        return emptyList()
    }

    private fun parseStatus(trailers: Trailers): Int? {
        return trailers[GRPC_STATUS_TRAILER]?.first()?.toIntOrNull()
    }

    private fun parseMessage(trailers: Trailers): ByteString {
        val message = trailers[GRPC_MESSAGE_TRAILER]?.first()
            ?: return ByteString.EMPTY
        return grpcPercentDecode(message).encodeUtf8()
    }

    // grpcPercentEncode/grpcPercentDecode follows RFC 3986 Section 2.1 and the gRPC HTTP/2 spec.
    // It's a variant of URL-encoding with fewer reserved characters. It's intended
    // to take UTF-8 encoded text and escape non-ASCII bytes so that they're valid
    // HTTP/1 headers, while still maximizing readability of the data on the wire.
    //
    // The grpc-message trailer (used for human-readable error messages) should be
    // percent-encoded.
    //
    // References:
    //
    // 	https://github.com/grpc/grpc/blob/master/doc/PROTOCOL-HTTP2.md#responses
    // 	https://datatracker.ietf.org/doc/html/rfc3986#section-2.1
    private fun grpcPercentDecode(encoded: String): String {
        for ((index, character) in encoded.withIndex()) {
            if (character == '%' && index + 2 < encoded.length) {
                return grpcPercentDecodeSlow(encoded, index)
            }
        }
        return encoded
    }

    private fun grpcPercentDecodeSlow(str: String, offset: Int): String {
        val encoded = str.encodeUtf8()
        val buffer = Buffer()
        try {
            buffer.write(encoded.substring(0, offset))
            var i = offset
            while (i < encoded.size) {
                val character = encoded[i].toInt().toChar()
                if (character != '%' || i + 2 > encoded.size) {
                    buffer.writeByte(character.code)
                    i++
                    continue
                }
                val parsed = encoded.substring(i + 1, i + 3).utf8().toUIntOrNull(16)
                if (parsed != null) {
                    buffer.writeByte(parsed.toInt())
                }
                i += 3
            }
            return buffer.readUtf8()
        } catch (e: Throwable) {
            throw e
        }
    }
}

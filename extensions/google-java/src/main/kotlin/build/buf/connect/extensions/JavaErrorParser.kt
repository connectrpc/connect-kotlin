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

package com.connectrpc.extensions

import build.buf.google.rpc.Status
import com.connectrpc.AnyError
import com.connectrpc.ConnectErrorDetail
import com.connectrpc.ErrorDetailParser
import com.google.protobuf.Message
import okio.ByteString.Companion.decodeBase64
import okio.ByteString.Companion.encodeUtf8
import kotlin.reflect.KClass

private const val TYPE_URL_PREFIX = "type.googleapis.com/"

internal object JavaErrorParser : ErrorDetailParser {
    /**
     * This unchecked cast is making the assumption that the caller
     * expects a generic type E? and the underlying type is a Google
     * Java Message.
     */
    @Suppress("UNCHECKED_CAST")
    override fun <E : Any> unpack(any: AnyError, clazz: KClass<E>): E? {
        val value = any.value.utf8().decodeBase64() ?: any.value
        val anyProto = com.google.protobuf.Any.newBuilder()
            .setTypeUrl(if (any.typeUrl.contains('/')) any.typeUrl else "$TYPE_URL_PREFIX${any.typeUrl}")
            .setValue(com.google.protobuf.ByteString.copyFrom(value.asByteBuffer()))
            .build()
        val kClass = clazz.java as Class<Message>
        val unpacked = anyProto.unpack(kClass)
        if (unpacked?.javaClass?.isAssignableFrom(clazz.java) == true) {
            return unpacked as E?
        }
        return null
    }

    override fun parseDetails(bytes: ByteArray): List<ConnectErrorDetail> {
        val status = Status.parseFrom(bytes)
        return status.detailsList.map { msg ->
            ConnectErrorDetail(
                msg.typeUrl,
                // Try to decode via base64 and if that fails, use the original value.
                // Connect unary ends up encoding the payload as base64. GRPC and GRPC-Web
                // both do not encode this payload as base64 so decodeBase64() returns null.
                msg.value.toStringUtf8().decodeBase64() ?: msg.value.toStringUtf8().encodeUtf8()
            )
        }
    }
}

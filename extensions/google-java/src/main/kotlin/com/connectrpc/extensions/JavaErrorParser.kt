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

package com.connectrpc.extensions

import com.connectrpc.AnyError
import com.connectrpc.ConnectErrorDetail
import com.connectrpc.ErrorDetailParser
import com.connectrpc.google.rpc.Status
import com.google.protobuf.Internal
import com.google.protobuf.Message
import okio.ByteString
import kotlin.reflect.KClass
import kotlin.reflect.cast
import kotlin.reflect.full.isSubclassOf

internal object JavaErrorParser : ErrorDetailParser {
    override fun <E : Any> unpack(any: AnyError, clazz: KClass<E>): E? {
        if (!clazz.isSubclassOf(Message::class)) {
            throw RuntimeException("class ${clazz.qualifiedName} does not extend Message")
        }
        @Suppress("UNCHECKED_CAST") // we just checked above, so it's safe
        val instance = Internal.getDefaultInstance(clazz.java as Class<Message>) as Message
        if (typeNameFromUrl(any.typeUrl) != instance.descriptorForType.fullName) {
            // TODO: should this throw instead?
            return null
        }
        val unpacked = instance.parserForType.parseFrom(any.value.toByteArray())
        return clazz.cast(unpacked)
    }

    override fun parseDetails(bytes: ByteArray): List<ConnectErrorDetail> {
        val status = Status.parseFrom(bytes)
        return status.detailsList.map { msg ->
            ConnectErrorDetail(
                type = msg.typeUrl,
                payload = ByteString.of(*msg.value.toByteArray()),
            )
        }
    }

    private fun typeNameFromUrl(url: String): String {
        return url.substring(url.lastIndexOf('/') + 1)
    }
}

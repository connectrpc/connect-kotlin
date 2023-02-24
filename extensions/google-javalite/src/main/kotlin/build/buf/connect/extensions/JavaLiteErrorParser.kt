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

package build.buf.connect.extensions

import build.buf.connect.AnyError
import build.buf.connect.ConnectErrorDetail
import build.buf.connect.ErrorDetailParser
import build.buf.google.rpc.Status
import com.google.protobuf.Internal
import com.google.protobuf.MessageLite
import okio.ByteString.Companion.decodeBase64
import okio.ByteString.Companion.encodeUtf8
import kotlin.reflect.KClass

internal object JavaLiteErrorParser : ErrorDetailParser {
    /**
     * This unchecked cast is making the assumption that the caller
     * expects a generic type E? and the underlying type is a Google
     * MessageLite.
     */
    @Suppress("UNCHECKED_CAST")
    override fun <E : Any> unpack(any: AnyError, clazz: KClass<E>): E? {
        val instance = Internal.getDefaultInstance(clazz.java as Class<MessageLite>) as MessageLite
        val value = any.value.utf8().decodeBase64() ?: any.value
        val unpacked = instance.parserForType.parseFrom(value.toByteArray())
        if (unpacked?.javaClass?.isAssignableFrom(clazz.java) == true) {
            return unpacked as E?
        }
        return null
    }

    override fun parseDetails(bytes: ByteArray): List<ConnectErrorDetail> {
        val status = Status.parseFrom(bytes)
        return status.detailsList.map { msg ->
            ConnectErrorDetail(
                type = msg.typeUrl,
                payload = msg.value.toStringUtf8().decodeBase64()
                    ?: msg.value.toStringUtf8().encodeUtf8()
            )
        }
    }
}

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

package com.connectrpc.ktor

import com.connectrpc.AnyError
import com.connectrpc.Codec
import com.connectrpc.ConnectErrorDetail
import com.connectrpc.ErrorDetailParser
import com.connectrpc.SerializationStrategy
import okio.Buffer
import okio.BufferedSource
import kotlin.reflect.KClass

/**
 * Test serialization strategy that handles String types.
 */
internal class KtorTestSerializationStrategy : SerializationStrategy {
    override fun serializationName(): String = "json"

    @Suppress("UNCHECKED_CAST")
    override fun <E : Any> codec(clazz: KClass<E>): Codec<E> {
        return KtorTestStringCodec() as Codec<E>
    }

    override fun errorDetailParser(): ErrorDetailParser {
        return KtorTestErrorDetailParser()
    }
}

/**
 * Simple codec that serializes strings as UTF-8.
 */
internal class KtorTestStringCodec : Codec<String> {
    override fun encodingName(): String = "json"

    override fun serialize(message: String): Buffer {
        return Buffer().writeUtf8(message)
    }

    override fun deterministicSerialize(message: String): Buffer {
        return serialize(message)
    }

    override fun deserialize(source: BufferedSource): String {
        return source.readUtf8()
    }
}

/**
 * Test error detail parser that returns empty list.
 */
internal class KtorTestErrorDetailParser : ErrorDetailParser {
    override fun <E : Any> unpack(any: AnyError, clazz: KClass<E>): E? = null
    override fun parseDetails(bytes: ByteArray): List<ConnectErrorDetail> = emptyList()
}

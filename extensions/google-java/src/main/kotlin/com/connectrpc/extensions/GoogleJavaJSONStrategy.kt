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

import com.connectrpc.CODEC_NAME_JSON
import com.connectrpc.Codec
import com.connectrpc.ErrorDetailParser
import com.connectrpc.SerializationStrategy
import com.google.protobuf.Message
import com.google.protobuf.TypeRegistry
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

/**
 * The Google Java JSON serialization strategy.
 */
class GoogleJavaJSONStrategy(
    private val registry: TypeRegistry = TypeRegistry.getEmptyTypeRegistry(),
) : SerializationStrategy {
    override fun serializationName(): String {
        return CODEC_NAME_JSON
    }

    override fun <E : Any> codec(clazz: KClass<E>): Codec<E> {
        if (!clazz.isSubclassOf(Message::class)) {
            throw RuntimeException("class ${clazz.qualifiedName} does not extend MessageLite")
        }
        @Suppress("UNCHECKED_CAST") // we just checked above, so it's safe
        val messageClass = clazz as KClass<out Message>

        @Suppress("UNCHECKED_CAST") // messageClass is actually KClass<E>, so it's safe
        val adapter = GoogleJavaJSONAdapter(messageClass, registry) as Codec<E>
        return adapter
    }

    override fun errorDetailParser(): ErrorDetailParser {
        return JavaErrorParser
    }
}

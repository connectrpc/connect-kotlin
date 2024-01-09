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

import com.connectrpc.CODEC_NAME_PROTO
import com.connectrpc.Codec
import com.google.protobuf.CodedOutputStream
import com.google.protobuf.ExtensionRegistryLite
import com.google.protobuf.Internal
import com.google.protobuf.MessageLite
import okio.Buffer
import okio.BufferedSource
import kotlin.reflect.KClass
import kotlin.reflect.cast

/**
 * Adapter to use Google's protobuf-java runtime for
 * deserializing and serializing data types.
 */
internal class GoogleJavaProtoAdapter<E : MessageLite>(
    private val clazz: KClass<E>,
    private val registry: ExtensionRegistryLite,
) : Codec<E> {
    private val instance by lazy {
        Internal.getDefaultInstance(clazz.java)
    }

    override fun encodingName(): String {
        return CODEC_NAME_PROTO
    }

    override fun deserialize(source: BufferedSource): E {
        return clazz.cast(instance.parserForType.parseFrom(source.inputStream(), registry))
    }

    override fun serialize(message: E): Buffer {
        return serialize(message, false)
    }

    override fun deterministicSerialize(message: E): Buffer {
        return serialize(message, true)
    }

    private fun serialize(message: E, deterministic: Boolean): Buffer {
        val result = ByteArray(message.serializedSize)
        val output = CodedOutputStream.newInstance(result)
        if (deterministic) {
            output.useDeterministicSerialization()
        }
        message.writeTo(output)
        output.checkNoSpaceLeft()
        return Buffer().write(result)
    }
}

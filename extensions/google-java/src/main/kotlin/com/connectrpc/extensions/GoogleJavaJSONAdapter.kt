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

package com.connectrpc.extensions

import com.connectrpc.CODEC_NAME_JSON
import com.connectrpc.Codec
import com.google.protobuf.Internal
import com.google.protobuf.Message
import com.google.protobuf.TypeRegistry
import com.google.protobuf.util.JsonFormat
import okio.Buffer
import okio.BufferedSource
import okio.ByteString.Companion.encodeUtf8
import kotlin.reflect.KClass
import kotlin.reflect.cast

/**
 * Adapter for Connect to use Google's protobuf-java runtime for
 * deserializing and serializing data types.
 */
internal class GoogleJavaJSONAdapter<E : Message>(
    private val clazz: KClass<E>,
    private val registry: TypeRegistry,
) : Codec<E> {
    private val instance by lazy {
        Internal.getDefaultInstance(clazz.java)
    }

    override fun encodingName(): String {
        return CODEC_NAME_JSON
    }

    override fun deserialize(source: BufferedSource): E {
        val builder = instance.newBuilderForType()
        JsonFormat.parser()
            .ignoringUnknownFields()
            .usingTypeRegistry(registry)
            .merge(source.readUtf8(), builder)
        return clazz.cast(builder.build())
    }

    override fun serialize(message: E): Buffer {
        return serialize(message, false)
    }

    override fun deterministicSerialize(message: E): Buffer {
        return serialize(message, true)
    }

    private fun serialize(message: E, deterministic: Boolean): Buffer {
        var printer = JsonFormat.printer()
        if (deterministic) {
            printer = printer.sortingMapKeys()
        }
        // TODO: It would likely be more efficient to use printer.appendTo
        //       with an Appendable implementation that wraps a Buffer.
        val jsonString = printer.print(message)
        return Buffer().write(jsonString.encodeUtf8())
    }
}

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

import com.connectrpc.Codec
import com.connectrpc.codecNameProto
import com.google.protobuf.CodedOutputStream
import com.google.protobuf.GeneratedMessageLite
import com.google.protobuf.Internal
import com.google.protobuf.MessageLite
import okio.Buffer
import okio.BufferedSource
import java.io.IOException
import kotlin.reflect.KClass

/**
 * Adapter to use Google's protobuf-javalite runtime for
 * deserializing and serializing data types.
 */
internal class GoogleLiteProtoAdapter<E : GeneratedMessageLite<out E, *>>(
    clazz: KClass<E>
) : Codec<E> {
    /**
     * Casting assumes the user is using Google's MessageLite type.
     */
    @Suppress("UNCHECKED_CAST")
    private val instance by lazy {
        Internal.getDefaultInstance(clazz.java as Class<MessageLite>) as E
    }

    override fun encodingName(): String {
        return codecNameProto
    }

    override fun deserialize(source: BufferedSource): E {
        return instance.parserForType.parseFrom(source.inputStream())
    }

    override fun serialize(message: E): Buffer {
        return Buffer().write(message.toByteArray())
    }

    override fun deterministicSerialize(message: E): Buffer {
        return try {
            val result = Buffer()
            val output = CodedOutputStream.newInstance(result.outputStream())
            output.useDeterministicSerialization()
            message.writeTo(output)
            output.checkNoSpaceLeft()
            result
        } catch (e: IOException) {
            throw RuntimeException("deterministic serialization failed", e)
        }
    }
}

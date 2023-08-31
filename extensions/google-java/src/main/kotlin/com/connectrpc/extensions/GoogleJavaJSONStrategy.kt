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

import com.connectrpc.Codec
import com.connectrpc.ErrorDetailParser
import com.connectrpc.SerializationStrategy
import com.connectrpc.codecNameJSON
import com.google.protobuf.GeneratedMessageV3
import kotlin.reflect.KClass

/**
 * The Google Java JSON serialization strategy.
 */
class GoogleJavaJSONStrategy : SerializationStrategy {
    override fun serializationName(): String {
        return codecNameJSON
    }

    /**
     * This unchecked cast assumes the underlying class type is
     * a Google GeneratedMessageV3.
     */
    @Suppress("UNCHECKED_CAST")
    override fun <E : Any> codec(clazz: KClass<E>): Codec<E> {
        val messageClass = clazz as KClass<GeneratedMessageV3>
        return GoogleJavaJSONAdapter(messageClass) as Codec<E>
    }

    override fun errorDetailParser(): ErrorDetailParser {
        return JavaErrorParser
    }
}

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

package com.connectrpc

import kotlin.reflect.KClass

/**
 * The serialization strategy for completion events from gRPC or Connect.
 *
 * A base data type will need to implement a [SerializationStrategy].
 */
interface SerializationStrategy {

    /**
     * The name of the serialization. Used in the content-type
     * header.
     */
    fun serializationName(): String

    /**
     * Get the Codec to serialize and deserialize a payload.
     */
    fun <E : Any> codec(clazz: KClass<E>): Codec<E>

    /**
     * @return The error detail parser for a specific base data type.
     */
    fun errorDetailParser(): ErrorDetailParser
}

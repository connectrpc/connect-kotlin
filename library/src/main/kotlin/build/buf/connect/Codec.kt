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

package build.buf.connect

import okio.Buffer
import okio.BufferedSource

const val codecNameProto = "proto"
const val codecNameJSON = "json"

/**
 * Defines a type that is capable of encoding and decoding messages using a specific format.
 */
interface Codec<E> {
    /**
     * @return The name of the codec's format (e.g., "json", "protobuf"). Usually consumed
     * in the form of adding the `content-type` header via "application/{name}".
     */
    fun encodingName(): String

    /**
     * Serializes the input message into the codec's format.
     *
     * @param message Typed input message.
     *
     * @return Serialized data that can be transmitted.
     */
    fun serialize(message: E): Buffer

    /**
     * Deterministic Serialization of the input message.
     *
     * @param message Typed input message.
     *
     * @return Deterministic serialization of the data.
     */
    fun deterministicSerialize(message: E): Buffer

    /**
     * Deserializes data in the codec's format into a typed message.
     *
     * @param source The source data to deserialize.
     *
     * @return The typed output message.
     */
    fun deserialize(source: BufferedSource): E
}

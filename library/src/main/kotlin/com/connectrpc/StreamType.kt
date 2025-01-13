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

/**
 * Represents the RPC stream type. Set by the code generator on each [MethodSpec].
 */
enum class StreamType {
    /** Unary RPC. */
    UNARY,

    /** Client streaming RPC. */
    CLIENT,

    /** Server streaming RPC. */
    SERVER,

    /** Bidirectional streaming RPC. */
    BIDI,
}

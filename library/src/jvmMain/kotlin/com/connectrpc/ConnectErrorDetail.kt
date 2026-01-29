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

import okio.ByteString

// A ConnectErrorDetail is a self-describing Protobuf message attached to a
// [ConnectException].
//
// Error details are sent over the network to clients, which can then work with
// strongly-typed data rather than trying to parse a complex error message. For
// example, you might use details to send a localized error message or retry
// parameters to the client.
//
// The [com.google.rpc](https://googleapis.github.io/googleapis/java/all/latest/apidocs/com/google/rpc/package-summary.html)
// package contains a variety of Protobuf messages commonly used as error details.
data class ConnectErrorDetail(
    val type: String,
    val payload: ByteString,
) {
    val pb = AnyError(type, payload)
}

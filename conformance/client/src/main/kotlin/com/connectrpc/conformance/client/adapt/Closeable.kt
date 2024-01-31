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

package com.connectrpc.conformance.client.adapt

// Like java.io.Closeable, but the close operation is suspendable.
interface Closeable {
    suspend fun close()
}

// Like the standard kotlin "use" extension function, but uses
// a suspending Closeable instead of java.io.Closeable and accepts
// a suspending block.
internal suspend fun <T : Closeable, R> T.use(block: suspend (T) -> R): R {
    var exception: Throwable? = null
    try {
        return block(this)
    } catch (ex: Throwable) {
        exception = ex
        throw exception
    } finally {
        try {
            this.close()
        } catch (ex: Throwable) {
            if (exception != null) {
                exception.addSuppressed(ex)
            } else {
                throw ex
            }
        }
    }
}

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

enum class Idempotency {
    /**
     * IdempotencyUnknown is the default idempotency level. A procedure with
     * this idempotency level may not be idempotent. This is appropriate for
     * any kind of procedure.
     */
    UNKNOWN,

    /**
     * IdempotencyNoSideEffects is the idempotency level that specifies that a
     * given call has no side-effects. This is equivalent to [RFC 9110 § 9.2.1]
     * "safe" methods in terms of semantics. This procedure should not mutate
     * any state. This idempotency level is appropriate for queries, or anything
     * that would be suitable for an HTTP GET request. In addition, due to the
     * lack of side-effects, such a procedure would be suitable to retry and
     * expect that the results will not be altered by preceding attempts.
     *
     * [RFC 9110 § 9.2.1]: https://www.rfc-editor.org/rfc/rfc9110.html#section-9.2.1
     */
    NO_SIDE_EFFECTS,

    /**
     * IdempotencyIdempotent is the idempotency level that specifies that a
     * given call is "idempotent", such that multiple instances of the same
     * request to this procedure would have the same side-effects as a single
     * request. This is equivalent to [RFC 9110 § 9.2.2] "idempotent" methods.
     * This level is a subset of the previous level. This idempotency level is
     * appropriate for any procedure that is safe to retry multiple times
     * and be guaranteed that the response and side-effects will not be altered
     * as a result of multiple attempts, for example, entity deletion requests.
     *
     * [RFC 9110 § 9.2.2]: https://www.rfc-editor.org/rfc/rfc9110.html#section-9.2.2
     */
    IDEMPOTENT,
}

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

// Code generated by connect-kotlin. DO NOT EDIT.
//
// Source: buf/javamultiplefiles/disabled/v1/disabled_empty.proto
//
package com.buf.javamultiplefiles.disabled.v1

import build.buf.connect.Headers
import build.buf.connect.ResponseMessage

public interface DisabledEmptyServiceClientInterface {
  public suspend fun disabledEmptyRPC(request: DisabledEmptyRPCRequest, headers: Headers =
      emptyMap()): ResponseMessage<DisabledEmptyRPCResponse>
}

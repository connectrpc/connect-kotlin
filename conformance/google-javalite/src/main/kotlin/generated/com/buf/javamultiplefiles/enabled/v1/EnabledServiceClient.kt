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
// Source: buf/javamultiplefiles/enabled/v1/enabled.proto
//
package com.buf.javamultiplefiles.enabled.v1

import build.buf.connect.Headers
import build.buf.connect.MethodSpec
import build.buf.connect.ProtocolClientInterface
import build.buf.connect.ResponseMessage

public class EnabledServiceClient(
  private val client: ProtocolClientInterface,
) : EnabledServiceClientInterface {
  public override suspend fun enabled(request: EnabledRequest, headers: Headers):
      ResponseMessage<EnabledResponse> = client.unary(
    request,
    headers,
    MethodSpec(
    "buf.javamultiplefiles.enabled.v1.EnabledService/Enabled",
      com.buf.javamultiplefiles.enabled.v1.EnabledRequest::class,
      com.buf.javamultiplefiles.enabled.v1.EnabledResponse::class,
    ),
  )

}

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
// Source: buf/javamultiplefiles/disabled/v1/disabled.proto
//
package buf.javamultiplefiles.disabled.v1

import build.buf.connect.Headers
import build.buf.connect.MethodSpec
import build.buf.connect.ProtocolClientInterface
import build.buf.connect.ResponseMessage
import build.buf.connect.http.Cancelable
import kotlin.Unit

public class DisabledServiceClient(
  private val client: ProtocolClientInterface,
) : DisabledServiceClientInterface {
  public override suspend fun disabled(request: Disabled.DisabledRequest, headers: Headers):
      ResponseMessage<Disabled.DisabledResponse> = client.unary(
    request,
    headers,
    MethodSpec(
    "buf.javamultiplefiles.disabled.v1.DisabledService/Disabled",
      buf.javamultiplefiles.disabled.v1.Disabled.DisabledRequest::class,
      buf.javamultiplefiles.disabled.v1.Disabled.DisabledResponse::class,
    ),
  )


  public override fun disabled(
    request: Disabled.DisabledRequest,
    headers: Headers,
    onResult: (ResponseMessage<Disabled.DisabledResponse>) -> Unit,
  ): Cancelable = client.unary(
    request,
    headers,
    MethodSpec(
    "buf.javamultiplefiles.disabled.v1.DisabledService/Disabled",
      buf.javamultiplefiles.disabled.v1.Disabled.DisabledRequest::class,
      buf.javamultiplefiles.disabled.v1.Disabled.DisabledResponse::class,
    ),
    onResult
  )

}

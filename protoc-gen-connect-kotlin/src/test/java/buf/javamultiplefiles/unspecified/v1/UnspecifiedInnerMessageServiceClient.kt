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
// Source: buf/javamultiplefiles/unspecified/v1/enabled_nested.proto
//
package buf.javamultiplefiles.unspecified.v1

import build.buf.connect.Headers
import build.buf.connect.MethodSpec
import build.buf.connect.ProtocolClientInterface
import build.buf.connect.ResponseMessage
import build.buf.connect.http.Cancelable
import kotlin.Unit

public class UnspecifiedInnerMessageServiceClient(
  private val client: ProtocolClientInterface,
) : UnspecifiedInnerMessageServiceClientInterface {
  /**
   *  buf:lint:ignore RPC_REQUEST_STANDARD_NAME
   *  buf:lint:ignore RPC_RESPONSE_STANDARD_NAME
   */
  public override suspend
      fun unspecifiedInnerMessageRPC(request: EnabledNested.UnspecifiedOuterMessageNested.InnerMessage,
      headers: Headers): ResponseMessage<EnabledNested.UnspecifiedOuterMessageNested.InnerMessage> =
      client.unary(
    request,
    headers,
    MethodSpec(
    "buf.javamultiplefiles.unspecified.v1.UnspecifiedInnerMessageService/UnspecifiedInnerMessageRPC",
      buf.javamultiplefiles.unspecified.v1.EnabledNested.UnspecifiedOuterMessageNested.InnerMessage::class,
      buf.javamultiplefiles.unspecified.v1.EnabledNested.UnspecifiedOuterMessageNested.InnerMessage::class,
    ),
  )


  /**
   *  buf:lint:ignore RPC_REQUEST_STANDARD_NAME
   *  buf:lint:ignore RPC_RESPONSE_STANDARD_NAME
   */
  public override fun unspecifiedInnerMessageRPC(
    request: EnabledNested.UnspecifiedOuterMessageNested.InnerMessage,
    headers: Headers,
    onResult: (ResponseMessage<EnabledNested.UnspecifiedOuterMessageNested.InnerMessage>) -> Unit,
  ): Cancelable = client.unary(
    request,
    headers,
    MethodSpec(
    "buf.javamultiplefiles.unspecified.v1.UnspecifiedInnerMessageService/UnspecifiedInnerMessageRPC",
      buf.javamultiplefiles.unspecified.v1.EnabledNested.UnspecifiedOuterMessageNested.InnerMessage::class,
      buf.javamultiplefiles.unspecified.v1.EnabledNested.UnspecifiedOuterMessageNested.InnerMessage::class,
    ),
    onResult
  )

}

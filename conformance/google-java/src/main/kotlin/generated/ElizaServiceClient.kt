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
// Source: no_package.proto
//
import build.buf.connect.Headers
import build.buf.connect.MethodSpec
import build.buf.connect.ProtocolClientInterface
import build.buf.connect.ResponseMessage
import build.buf.connect.UnaryBlockingCall
import build.buf.connect.http.Cancelable
import kotlin.Unit

public class ElizaServiceClient(
  private val client: ProtocolClientInterface,
) : ElizaServiceClientInterface {
  public override suspend fun say(request: SayRequest, headers: Headers):
      ResponseMessage<SayResponse> = client.unary(
    request,
    headers,
    MethodSpec(
    "ElizaService/Say",
      SayRequest::class,
      SayResponse::class,
    ),
  )


  public override fun say(
    request: SayRequest,
    headers: Headers,
    onResult: (ResponseMessage<SayResponse>) -> Unit,
  ): Cancelable = client.unary(
    request,
    headers,
    MethodSpec(
    "ElizaService/Say",
      SayRequest::class,
      SayResponse::class,
    ),
    onResult
  )


  public override fun sayBlocking(request: SayRequest, headers: Headers):
      UnaryBlockingCall<SayResponse> = client.unaryBlocking(
    request,
    headers,
    MethodSpec(
    "ElizaService/Say",
      SayRequest::class,
      SayResponse::class,
    ),
  )

}

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
// Source: grpc/testing/test.proto
//
package com.grpc.testing

import build.buf.connect.Headers
import build.buf.connect.MethodSpec
import build.buf.connect.ProtocolClientInterface
import build.buf.connect.ResponseMessage
import build.buf.connect.UnaryBlockingCall
import build.buf.connect.http.Cancelable
import kotlin.Unit

/**
 *  A service to dynamically update the configuration of an xDS test client.
 */
public class XdsUpdateClientConfigureServiceClient(
  private val client: ProtocolClientInterface,
) : XdsUpdateClientConfigureServiceClientInterface {
  /**
   *  Update the tes client's configuration.
   */
  public override suspend fun configure(request: ClientConfigureRequest, headers: Headers):
      ResponseMessage<ClientConfigureResponse> = client.unary(
    request,
    headers,
    MethodSpec(
    "grpc.testing.XdsUpdateClientConfigureService/Configure",
      com.grpc.testing.ClientConfigureRequest::class,
      com.grpc.testing.ClientConfigureResponse::class,
    ),
  )


  /**
   *  Update the tes client's configuration.
   */
  public override fun configure(
    request: ClientConfigureRequest,
    headers: Headers,
    onResult: (ResponseMessage<ClientConfigureResponse>) -> Unit,
  ): Cancelable = client.unary(
    request,
    headers,
    MethodSpec(
    "grpc.testing.XdsUpdateClientConfigureService/Configure",
      com.grpc.testing.ClientConfigureRequest::class,
      com.grpc.testing.ClientConfigureResponse::class,
    ),
    onResult
  )


  /**
   *  Update the tes client's configuration.
   */
  public override fun configureBlocking(request: ClientConfigureRequest, headers: Headers):
      UnaryBlockingCall<ClientConfigureResponse> = client.unaryBlocking(
    request,
    headers,
    MethodSpec(
    "grpc.testing.XdsUpdateClientConfigureService/Configure",
      com.grpc.testing.ClientConfigureRequest::class,
      com.grpc.testing.ClientConfigureResponse::class,
    ),
  )

}

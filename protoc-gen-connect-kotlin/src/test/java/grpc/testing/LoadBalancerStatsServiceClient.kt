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
package grpc.testing

import build.buf.connect.Headers
import build.buf.connect.MethodSpec
import build.buf.connect.ProtocolClientInterface
import build.buf.connect.ResponseMessage
import build.buf.connect.http.Cancelable
import kotlin.Unit

/**
 *  A service used to obtain stats for verifying LB behavior.
 */
public class LoadBalancerStatsServiceClient(
  private val client: ProtocolClientInterface,
) : LoadBalancerStatsServiceClientInterface {
  /**
   *  Gets the backend distribution for RPCs sent by a test client.
   */
  public override suspend fun getClientStats(request: Messages.LoadBalancerStatsRequest,
      headers: Headers): ResponseMessage<Messages.LoadBalancerStatsResponse> = client.unary(
    request,
    headers,
    MethodSpec(
    "grpc.testing.LoadBalancerStatsService/GetClientStats",
      grpc.testing.Messages.LoadBalancerStatsRequest::class,
      grpc.testing.Messages.LoadBalancerStatsResponse::class,
    ),
  )


  /**
   *  Gets the backend distribution for RPCs sent by a test client.
   */
  public override fun getClientStats(
    request: Messages.LoadBalancerStatsRequest,
    headers: Headers,
    onResult: (ResponseMessage<Messages.LoadBalancerStatsResponse>) -> Unit,
  ): Cancelable = client.unary(
    request,
    headers,
    MethodSpec(
    "grpc.testing.LoadBalancerStatsService/GetClientStats",
      grpc.testing.Messages.LoadBalancerStatsRequest::class,
      grpc.testing.Messages.LoadBalancerStatsResponse::class,
    ),
    onResult
  )


  /**
   *  Gets the accumulated stats for RPCs sent by a test client.
   */
  public override suspend
      fun getClientAccumulatedStats(request: Messages.LoadBalancerAccumulatedStatsRequest,
      headers: Headers): ResponseMessage<Messages.LoadBalancerAccumulatedStatsResponse> =
      client.unary(
    request,
    headers,
    MethodSpec(
    "grpc.testing.LoadBalancerStatsService/GetClientAccumulatedStats",
      grpc.testing.Messages.LoadBalancerAccumulatedStatsRequest::class,
      grpc.testing.Messages.LoadBalancerAccumulatedStatsResponse::class,
    ),
  )


  /**
   *  Gets the accumulated stats for RPCs sent by a test client.
   */
  public override fun getClientAccumulatedStats(
    request: Messages.LoadBalancerAccumulatedStatsRequest,
    headers: Headers,
    onResult: (ResponseMessage<Messages.LoadBalancerAccumulatedStatsResponse>) -> Unit,
  ): Cancelable = client.unary(
    request,
    headers,
    MethodSpec(
    "grpc.testing.LoadBalancerStatsService/GetClientAccumulatedStats",
      grpc.testing.Messages.LoadBalancerAccumulatedStatsRequest::class,
      grpc.testing.Messages.LoadBalancerAccumulatedStatsResponse::class,
    ),
    onResult
  )

}

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
// Source: buf/evilcomments/v1/evilcomments.proto
//
package com.buf.evilcomments.v1

import build.buf.connect.Headers
import build.buf.connect.MethodSpec
import build.buf.connect.ProtocolClientInterface
import build.buf.connect.ResponseMessage
import build.buf.connect.UnaryBlockingCall
import build.buf.connect.http.Cancelable
import kotlin.Unit

public class EvilCommentsServiceClient(
  private val client: ProtocolClientInterface,
) : EvilCommentsServiceClientInterface {
  /**
   *  This comment contains characters that should be escaped.
   *  &#64; is valid in KDoc, but not in proto.
   *  Comments in KDoc use C-style block comments, so &#42;/ and /&#42; should be escaped.
   *  &#91; and &#93; characters should also be escaped.
   */
  public override suspend fun evilComments(request: EvilCommentsRequest, headers: Headers):
      ResponseMessage<EvilCommentsResponse> = client.unary(
    request,
    headers,
    MethodSpec(
    "buf.evilcomments.v1.EvilCommentsService/EvilComments",
      com.buf.evilcomments.v1.EvilCommentsRequest::class,
      com.buf.evilcomments.v1.EvilCommentsResponse::class,
    ),
  )


  /**
   *  This comment contains characters that should be escaped.
   *  &#64; is valid in KDoc, but not in proto.
   *  Comments in KDoc use C-style block comments, so &#42;/ and /&#42; should be escaped.
   *  &#91; and &#93; characters should also be escaped.
   */
  public override fun evilComments(
    request: EvilCommentsRequest,
    headers: Headers,
    onResult: (ResponseMessage<EvilCommentsResponse>) -> Unit,
  ): Cancelable = client.unary(
    request,
    headers,
    MethodSpec(
    "buf.evilcomments.v1.EvilCommentsService/EvilComments",
      com.buf.evilcomments.v1.EvilCommentsRequest::class,
      com.buf.evilcomments.v1.EvilCommentsResponse::class,
    ),
    onResult
  )


  /**
   *  This comment contains characters that should be escaped.
   *  &#64; is valid in KDoc, but not in proto.
   *  Comments in KDoc use C-style block comments, so &#42;/ and /&#42; should be escaped.
   *  &#91; and &#93; characters should also be escaped.
   */
  public override fun evilCommentsBlocking(request: EvilCommentsRequest, headers: Headers):
      UnaryBlockingCall<EvilCommentsResponse> = client.unaryBlocking(
    request,
    headers,
    MethodSpec(
    "buf.evilcomments.v1.EvilCommentsService/EvilComments",
      com.buf.evilcomments.v1.EvilCommentsRequest::class,
      com.buf.evilcomments.v1.EvilCommentsResponse::class,
    ),
  )

}

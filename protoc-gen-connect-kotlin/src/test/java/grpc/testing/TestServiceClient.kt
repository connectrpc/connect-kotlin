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

import build.buf.connect.BidirectionalStreamInterface
import build.buf.connect.ClientOnlyStreamInterface
import build.buf.connect.Headers
import build.buf.connect.Idempotency.NO_SIDE_EFFECTS
import build.buf.connect.MethodSpec
import build.buf.connect.ProtocolClientInterface
import build.buf.connect.ResponseMessage
import build.buf.connect.ServerOnlyStreamInterface
import build.buf.connect.http.Cancelable
import kotlin.Unit

/**
 *  A simple service to test the various types of RPCs and experiment with
 *  performance with various types of payload.
 */
public class TestServiceClient(
  private val client: ProtocolClientInterface,
) : TestServiceClientInterface {
  /**
   *  One empty request followed by one empty response.
   */
  public override suspend fun emptyCall(request: EmptyOuterClass.Empty, headers: Headers):
      ResponseMessage<EmptyOuterClass.Empty> = client.unary(
    request,
    headers,
    MethodSpec(
    "grpc.testing.TestService/EmptyCall",
      grpc.testing.EmptyOuterClass.Empty::class,
      grpc.testing.EmptyOuterClass.Empty::class,
    ),
  )


  /**
   *  One empty request followed by one empty response.
   */
  public override fun emptyCall(
    request: EmptyOuterClass.Empty,
    headers: Headers,
    onResult: (ResponseMessage<EmptyOuterClass.Empty>) -> Unit,
  ): Cancelable = client.unary(
    request,
    headers,
    MethodSpec(
    "grpc.testing.TestService/EmptyCall",
      grpc.testing.EmptyOuterClass.Empty::class,
      grpc.testing.EmptyOuterClass.Empty::class,
    ),
    onResult
  )


  /**
   *  One request followed by one response.
   */
  public override suspend fun unaryCall(request: Messages.SimpleRequest, headers: Headers):
      ResponseMessage<Messages.SimpleResponse> = client.unary(
    request,
    headers,
    MethodSpec(
    "grpc.testing.TestService/UnaryCall",
      grpc.testing.Messages.SimpleRequest::class,
      grpc.testing.Messages.SimpleResponse::class,
    ),
  )


  /**
   *  One request followed by one response.
   */
  public override fun unaryCall(
    request: Messages.SimpleRequest,
    headers: Headers,
    onResult: (ResponseMessage<Messages.SimpleResponse>) -> Unit,
  ): Cancelable = client.unary(
    request,
    headers,
    MethodSpec(
    "grpc.testing.TestService/UnaryCall",
      grpc.testing.Messages.SimpleRequest::class,
      grpc.testing.Messages.SimpleResponse::class,
    ),
    onResult
  )


  /**
   *  One request followed by one response. This RPC always fails.
   */
  public override suspend fun failUnaryCall(request: Messages.SimpleRequest, headers: Headers):
      ResponseMessage<Messages.SimpleResponse> = client.unary(
    request,
    headers,
    MethodSpec(
    "grpc.testing.TestService/FailUnaryCall",
      grpc.testing.Messages.SimpleRequest::class,
      grpc.testing.Messages.SimpleResponse::class,
    ),
  )


  /**
   *  One request followed by one response. This RPC always fails.
   */
  public override fun failUnaryCall(
    request: Messages.SimpleRequest,
    headers: Headers,
    onResult: (ResponseMessage<Messages.SimpleResponse>) -> Unit,
  ): Cancelable = client.unary(
    request,
    headers,
    MethodSpec(
    "grpc.testing.TestService/FailUnaryCall",
      grpc.testing.Messages.SimpleRequest::class,
      grpc.testing.Messages.SimpleResponse::class,
    ),
    onResult
  )


  /**
   *  One request followed by one response. Response has cache control
   *  headers set such that a caching HTTP proxy (such as GFE) can
   *  satisfy subsequent requests.
   */
  public override suspend fun cacheableUnaryCall(request: Messages.SimpleRequest, headers: Headers):
      ResponseMessage<Messages.SimpleResponse> = client.unary(
    request,
    headers,
    MethodSpec(
    "grpc.testing.TestService/CacheableUnaryCall",
      grpc.testing.Messages.SimpleRequest::class,
      grpc.testing.Messages.SimpleResponse::class,
      NO_SIDE_EFFECTS
    ),
  )


  /**
   *  One request followed by one response. Response has cache control
   *  headers set such that a caching HTTP proxy (such as GFE) can
   *  satisfy subsequent requests.
   */
  public override fun cacheableUnaryCall(
    request: Messages.SimpleRequest,
    headers: Headers,
    onResult: (ResponseMessage<Messages.SimpleResponse>) -> Unit,
  ): Cancelable = client.unary(
    request,
    headers,
    MethodSpec(
    "grpc.testing.TestService/CacheableUnaryCall",
      grpc.testing.Messages.SimpleRequest::class,
      grpc.testing.Messages.SimpleResponse::class,
      NO_SIDE_EFFECTS
    ),
    onResult
  )


  /**
   *  One request followed by a sequence of responses (streamed download).
   *  The server returns the payload with client desired type and sizes.
   */
  public override suspend fun streamingOutputCall(headers: Headers):
      ServerOnlyStreamInterface<Messages.StreamingOutputCallRequest, Messages.StreamingOutputCallResponse>
      = client.serverStream(
    headers,
    MethodSpec(
    "grpc.testing.TestService/StreamingOutputCall",
      grpc.testing.Messages.StreamingOutputCallRequest::class,
      grpc.testing.Messages.StreamingOutputCallResponse::class,
    ),
  )


  /**
   *  One request followed by a sequence of responses (streamed download).
   *  The server returns the payload with client desired type and sizes.
   *  This RPC always responds with an error status.
   */
  public override suspend fun failStreamingOutputCall(headers: Headers):
      ServerOnlyStreamInterface<Messages.StreamingOutputCallRequest, Messages.StreamingOutputCallResponse>
      = client.serverStream(
    headers,
    MethodSpec(
    "grpc.testing.TestService/FailStreamingOutputCall",
      grpc.testing.Messages.StreamingOutputCallRequest::class,
      grpc.testing.Messages.StreamingOutputCallResponse::class,
    ),
  )


  /**
   *  A sequence of requests followed by one response (streamed upload).
   *  The server returns the aggregated size of client payload as the result.
   */
  public override suspend fun streamingInputCall(headers: Headers):
      ClientOnlyStreamInterface<Messages.StreamingInputCallRequest, Messages.StreamingInputCallResponse>
      = client.clientStream(
    headers,
    MethodSpec(
    "grpc.testing.TestService/StreamingInputCall",
      grpc.testing.Messages.StreamingInputCallRequest::class,
      grpc.testing.Messages.StreamingInputCallResponse::class,
    ),
  )


  /**
   *  A sequence of requests with each request served by the server immediately.
   *  As one request could lead to multiple responses, this interface
   *  demonstrates the idea of full duplexing.
   */
  public override suspend fun fullDuplexCall(headers: Headers):
      BidirectionalStreamInterface<Messages.StreamingOutputCallRequest, Messages.StreamingOutputCallResponse>
      = client.stream(
    headers,
    MethodSpec(
    "grpc.testing.TestService/FullDuplexCall",
      grpc.testing.Messages.StreamingOutputCallRequest::class,
      grpc.testing.Messages.StreamingOutputCallResponse::class,
    ),
  )


  /**
   *  A sequence of requests followed by a sequence of responses.
   *  The server buffers all the client requests and then serves them in order. A
   *  stream of responses are returned to the client when the server starts with
   *  first request.
   */
  public override suspend fun halfDuplexCall(headers: Headers):
      BidirectionalStreamInterface<Messages.StreamingOutputCallRequest, Messages.StreamingOutputCallResponse>
      = client.stream(
    headers,
    MethodSpec(
    "grpc.testing.TestService/HalfDuplexCall",
      grpc.testing.Messages.StreamingOutputCallRequest::class,
      grpc.testing.Messages.StreamingOutputCallResponse::class,
    ),
  )


  /**
   *  The test server will not implement this method. It will be used
   *  to test the behavior when clients call unimplemented methods.
   */
  public override suspend fun unimplementedCall(request: EmptyOuterClass.Empty, headers: Headers):
      ResponseMessage<EmptyOuterClass.Empty> = client.unary(
    request,
    headers,
    MethodSpec(
    "grpc.testing.TestService/UnimplementedCall",
      grpc.testing.EmptyOuterClass.Empty::class,
      grpc.testing.EmptyOuterClass.Empty::class,
    ),
  )


  /**
   *  The test server will not implement this method. It will be used
   *  to test the behavior when clients call unimplemented methods.
   */
  public override fun unimplementedCall(
    request: EmptyOuterClass.Empty,
    headers: Headers,
    onResult: (ResponseMessage<EmptyOuterClass.Empty>) -> Unit,
  ): Cancelable = client.unary(
    request,
    headers,
    MethodSpec(
    "grpc.testing.TestService/UnimplementedCall",
      grpc.testing.EmptyOuterClass.Empty::class,
      grpc.testing.EmptyOuterClass.Empty::class,
    ),
    onResult
  )


  /**
   *  The test server will not implement this method. It will be used
   *  to test the behavior when clients call unimplemented streaming output methods.
   */
  public override suspend fun unimplementedStreamingOutputCall(headers: Headers):
      ServerOnlyStreamInterface<EmptyOuterClass.Empty, EmptyOuterClass.Empty> = client.serverStream(
    headers,
    MethodSpec(
    "grpc.testing.TestService/UnimplementedStreamingOutputCall",
      grpc.testing.EmptyOuterClass.Empty::class,
      grpc.testing.EmptyOuterClass.Empty::class,
    ),
  )

}

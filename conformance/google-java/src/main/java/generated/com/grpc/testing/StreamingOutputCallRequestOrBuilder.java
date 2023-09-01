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

// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: grpc/testing/messages.proto

package com.grpc.testing;

public interface StreamingOutputCallRequestOrBuilder extends
    // @@protoc_insertion_point(interface_extends:grpc.testing.StreamingOutputCallRequest)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <pre>
   * Desired payload type in the response from the server.
   * If response_type is RANDOM, the payload from each response in the stream
   * might be of different types. This is to simulate a mixed type of payload
   * stream.
   * </pre>
   *
   * <code>.grpc.testing.PayloadType response_type = 1 [json_name = "responseType"];</code>
   * @return The enum numeric value on the wire for responseType.
   */
  int getResponseTypeValue();
  /**
   * <pre>
   * Desired payload type in the response from the server.
   * If response_type is RANDOM, the payload from each response in the stream
   * might be of different types. This is to simulate a mixed type of payload
   * stream.
   * </pre>
   *
   * <code>.grpc.testing.PayloadType response_type = 1 [json_name = "responseType"];</code>
   * @return The responseType.
   */
  com.grpc.testing.PayloadType getResponseType();

  /**
   * <pre>
   * Configuration for each expected response message.
   * </pre>
   *
   * <code>repeated .grpc.testing.ResponseParameters response_parameters = 2 [json_name = "responseParameters"];</code>
   */
  java.util.List<com.grpc.testing.ResponseParameters> 
      getResponseParametersList();
  /**
   * <pre>
   * Configuration for each expected response message.
   * </pre>
   *
   * <code>repeated .grpc.testing.ResponseParameters response_parameters = 2 [json_name = "responseParameters"];</code>
   */
  com.grpc.testing.ResponseParameters getResponseParameters(int index);
  /**
   * <pre>
   * Configuration for each expected response message.
   * </pre>
   *
   * <code>repeated .grpc.testing.ResponseParameters response_parameters = 2 [json_name = "responseParameters"];</code>
   */
  int getResponseParametersCount();
  /**
   * <pre>
   * Configuration for each expected response message.
   * </pre>
   *
   * <code>repeated .grpc.testing.ResponseParameters response_parameters = 2 [json_name = "responseParameters"];</code>
   */
  java.util.List<? extends com.grpc.testing.ResponseParametersOrBuilder> 
      getResponseParametersOrBuilderList();
  /**
   * <pre>
   * Configuration for each expected response message.
   * </pre>
   *
   * <code>repeated .grpc.testing.ResponseParameters response_parameters = 2 [json_name = "responseParameters"];</code>
   */
  com.grpc.testing.ResponseParametersOrBuilder getResponseParametersOrBuilder(
      int index);

  /**
   * <pre>
   * Optional input payload sent along with the request.
   * </pre>
   *
   * <code>.grpc.testing.Payload payload = 3 [json_name = "payload"];</code>
   * @return Whether the payload field is set.
   */
  boolean hasPayload();
  /**
   * <pre>
   * Optional input payload sent along with the request.
   * </pre>
   *
   * <code>.grpc.testing.Payload payload = 3 [json_name = "payload"];</code>
   * @return The payload.
   */
  com.grpc.testing.Payload getPayload();
  /**
   * <pre>
   * Optional input payload sent along with the request.
   * </pre>
   *
   * <code>.grpc.testing.Payload payload = 3 [json_name = "payload"];</code>
   */
  com.grpc.testing.PayloadOrBuilder getPayloadOrBuilder();

  /**
   * <pre>
   * Whether server should return a given status
   * </pre>
   *
   * <code>.grpc.testing.EchoStatus response_status = 7 [json_name = "responseStatus"];</code>
   * @return Whether the responseStatus field is set.
   */
  boolean hasResponseStatus();
  /**
   * <pre>
   * Whether server should return a given status
   * </pre>
   *
   * <code>.grpc.testing.EchoStatus response_status = 7 [json_name = "responseStatus"];</code>
   * @return The responseStatus.
   */
  com.grpc.testing.EchoStatus getResponseStatus();
  /**
   * <pre>
   * Whether server should return a given status
   * </pre>
   *
   * <code>.grpc.testing.EchoStatus response_status = 7 [json_name = "responseStatus"];</code>
   */
  com.grpc.testing.EchoStatusOrBuilder getResponseStatusOrBuilder();
}

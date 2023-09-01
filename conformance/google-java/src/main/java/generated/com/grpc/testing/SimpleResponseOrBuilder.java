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

public interface SimpleResponseOrBuilder extends
    // @@protoc_insertion_point(interface_extends:grpc.testing.SimpleResponse)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <pre>
   * Payload to increase message size.
   * </pre>
   *
   * <code>.grpc.testing.Payload payload = 1 [json_name = "payload"];</code>
   * @return Whether the payload field is set.
   */
  boolean hasPayload();
  /**
   * <pre>
   * Payload to increase message size.
   * </pre>
   *
   * <code>.grpc.testing.Payload payload = 1 [json_name = "payload"];</code>
   * @return The payload.
   */
  com.grpc.testing.Payload getPayload();
  /**
   * <pre>
   * Payload to increase message size.
   * </pre>
   *
   * <code>.grpc.testing.Payload payload = 1 [json_name = "payload"];</code>
   */
  com.grpc.testing.PayloadOrBuilder getPayloadOrBuilder();

  /**
   * <pre>
   * The user the request came from, for verifying authentication was
   * successful when the client expected it.
   * </pre>
   *
   * <code>string username = 2 [json_name = "username"];</code>
   * @return The username.
   */
  java.lang.String getUsername();
  /**
   * <pre>
   * The user the request came from, for verifying authentication was
   * successful when the client expected it.
   * </pre>
   *
   * <code>string username = 2 [json_name = "username"];</code>
   * @return The bytes for username.
   */
  com.google.protobuf.ByteString
      getUsernameBytes();

  /**
   * <pre>
   * OAuth scope.
   * </pre>
   *
   * <code>string oauth_scope = 3 [json_name = "oauthScope"];</code>
   * @return The oauthScope.
   */
  java.lang.String getOauthScope();
  /**
   * <pre>
   * OAuth scope.
   * </pre>
   *
   * <code>string oauth_scope = 3 [json_name = "oauthScope"];</code>
   * @return The bytes for oauthScope.
   */
  com.google.protobuf.ByteString
      getOauthScopeBytes();

  /**
   * <pre>
   * Server ID. This must be unique among different server instances,
   * but the same across all RPC's made to a particular server instance.
   * </pre>
   *
   * <code>string server_id = 4 [json_name = "serverId"];</code>
   * @return The serverId.
   */
  java.lang.String getServerId();
  /**
   * <pre>
   * Server ID. This must be unique among different server instances,
   * but the same across all RPC's made to a particular server instance.
   * </pre>
   *
   * <code>string server_id = 4 [json_name = "serverId"];</code>
   * @return The bytes for serverId.
   */
  com.google.protobuf.ByteString
      getServerIdBytes();

  /**
   * <pre>
   * gRPCLB Path.
   * </pre>
   *
   * <code>.grpc.testing.GrpclbRouteType grpclb_route_type = 5 [json_name = "grpclbRouteType"];</code>
   * @return The enum numeric value on the wire for grpclbRouteType.
   */
  int getGrpclbRouteTypeValue();
  /**
   * <pre>
   * gRPCLB Path.
   * </pre>
   *
   * <code>.grpc.testing.GrpclbRouteType grpclb_route_type = 5 [json_name = "grpclbRouteType"];</code>
   * @return The grpclbRouteType.
   */
  com.grpc.testing.GrpclbRouteType getGrpclbRouteType();

  /**
   * <pre>
   * Server hostname.
   * </pre>
   *
   * <code>string hostname = 6 [json_name = "hostname"];</code>
   * @return The hostname.
   */
  java.lang.String getHostname();
  /**
   * <pre>
   * Server hostname.
   * </pre>
   *
   * <code>string hostname = 6 [json_name = "hostname"];</code>
   * @return The bytes for hostname.
   */
  com.google.protobuf.ByteString
      getHostnameBytes();
}

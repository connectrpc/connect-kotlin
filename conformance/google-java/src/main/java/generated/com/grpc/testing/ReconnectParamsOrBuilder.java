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

public interface ReconnectParamsOrBuilder extends
    // @@protoc_insertion_point(interface_extends:grpc.testing.ReconnectParams)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <code>int32 max_reconnect_backoff_ms = 1 [json_name = "maxReconnectBackoffMs"];</code>
   * @return The maxReconnectBackoffMs.
   */
  int getMaxReconnectBackoffMs();
}

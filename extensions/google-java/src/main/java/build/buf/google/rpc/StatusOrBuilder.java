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
// source: google/rpc/status.proto

package build.buf.google.rpc;

public interface StatusOrBuilder extends
    // @@protoc_insertion_point(interface_extends:google.rpc.Status)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <pre>
   * The status code, which should be an enum value of
   * [google.rpc.Code][google.rpc.Code].
   * </pre>
   *
   * <code>int32 code = 1 [json_name = "code"];</code>
   * @return The code.
   */
  int getCode();

  /**
   * <pre>
   * A developer-facing error message, which should be in English. Any
   * user-facing error message should be localized and sent in the
   * [google.rpc.Status.details][google.rpc.Status.details] field, or localized
   * by the client.
   * </pre>
   *
   * <code>string message = 2 [json_name = "message"];</code>
   * @return The message.
   */
  java.lang.String getMessage();
  /**
   * <pre>
   * A developer-facing error message, which should be in English. Any
   * user-facing error message should be localized and sent in the
   * [google.rpc.Status.details][google.rpc.Status.details] field, or localized
   * by the client.
   * </pre>
   *
   * <code>string message = 2 [json_name = "message"];</code>
   * @return The bytes for message.
   */
  com.google.protobuf.ByteString
      getMessageBytes();

  /**
   * <pre>
   * A list of messages that carry the error details.  There is a common set of
   * message types for APIs to use.
   * </pre>
   *
   * <code>repeated .google.protobuf.Any details = 3 [json_name = "details"];</code>
   */
  java.util.List<com.google.protobuf.Any> 
      getDetailsList();
  /**
   * <pre>
   * A list of messages that carry the error details.  There is a common set of
   * message types for APIs to use.
   * </pre>
   *
   * <code>repeated .google.protobuf.Any details = 3 [json_name = "details"];</code>
   */
  com.google.protobuf.Any getDetails(int index);
  /**
   * <pre>
   * A list of messages that carry the error details.  There is a common set of
   * message types for APIs to use.
   * </pre>
   *
   * <code>repeated .google.protobuf.Any details = 3 [json_name = "details"];</code>
   */
  int getDetailsCount();
  /**
   * <pre>
   * A list of messages that carry the error details.  There is a common set of
   * message types for APIs to use.
   * </pre>
   *
   * <code>repeated .google.protobuf.Any details = 3 [json_name = "details"];</code>
   */
  java.util.List<? extends com.google.protobuf.AnyOrBuilder> 
      getDetailsOrBuilderList();
  /**
   * <pre>
   * A list of messages that carry the error details.  There is a common set of
   * message types for APIs to use.
   * </pre>
   *
   * <code>repeated .google.protobuf.Any details = 3 [json_name = "details"];</code>
   */
  com.google.protobuf.AnyOrBuilder getDetailsOrBuilder(
      int index);
}

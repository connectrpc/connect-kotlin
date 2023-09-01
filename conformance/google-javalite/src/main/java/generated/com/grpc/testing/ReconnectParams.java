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

/**
 * <pre>
 * For reconnect interop test only.
 * Client tells server what reconnection parameters it used.
 * </pre>
 *
 * Protobuf type {@code grpc.testing.ReconnectParams}
 */
public  final class ReconnectParams extends
    com.google.protobuf.GeneratedMessageLite<
        ReconnectParams, ReconnectParams.Builder> implements
    // @@protoc_insertion_point(message_implements:grpc.testing.ReconnectParams)
    ReconnectParamsOrBuilder {
  private ReconnectParams() {
  }
  public static final int MAX_RECONNECT_BACKOFF_MS_FIELD_NUMBER = 1;
  private int maxReconnectBackoffMs_;
  /**
   * <code>int32 max_reconnect_backoff_ms = 1 [json_name = "maxReconnectBackoffMs"];</code>
   * @return The maxReconnectBackoffMs.
   */
  @java.lang.Override
  public int getMaxReconnectBackoffMs() {
    return maxReconnectBackoffMs_;
  }
  /**
   * <code>int32 max_reconnect_backoff_ms = 1 [json_name = "maxReconnectBackoffMs"];</code>
   * @param value The maxReconnectBackoffMs to set.
   */
  private void setMaxReconnectBackoffMs(int value) {
    
    maxReconnectBackoffMs_ = value;
  }
  /**
   * <code>int32 max_reconnect_backoff_ms = 1 [json_name = "maxReconnectBackoffMs"];</code>
   */
  private void clearMaxReconnectBackoffMs() {
    
    maxReconnectBackoffMs_ = 0;
  }

  public static com.grpc.testing.ReconnectParams parseFrom(
      java.nio.ByteBuffer data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, data);
  }
  public static com.grpc.testing.ReconnectParams parseFrom(
      java.nio.ByteBuffer data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, data, extensionRegistry);
  }
  public static com.grpc.testing.ReconnectParams parseFrom(
      com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, data);
  }
  public static com.grpc.testing.ReconnectParams parseFrom(
      com.google.protobuf.ByteString data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, data, extensionRegistry);
  }
  public static com.grpc.testing.ReconnectParams parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, data);
  }
  public static com.grpc.testing.ReconnectParams parseFrom(
      byte[] data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, data, extensionRegistry);
  }
  public static com.grpc.testing.ReconnectParams parseFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, input);
  }
  public static com.grpc.testing.ReconnectParams parseFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, input, extensionRegistry);
  }
  public static com.grpc.testing.ReconnectParams parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    return parseDelimitedFrom(DEFAULT_INSTANCE, input);
  }
  public static com.grpc.testing.ReconnectParams parseDelimitedFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return parseDelimitedFrom(DEFAULT_INSTANCE, input, extensionRegistry);
  }
  public static com.grpc.testing.ReconnectParams parseFrom(
      com.google.protobuf.CodedInputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, input);
  }
  public static com.grpc.testing.ReconnectParams parseFrom(
      com.google.protobuf.CodedInputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, input, extensionRegistry);
  }

  public static Builder newBuilder() {
    return (Builder) DEFAULT_INSTANCE.createBuilder();
  }
  public static Builder newBuilder(com.grpc.testing.ReconnectParams prototype) {
    return (Builder) DEFAULT_INSTANCE.createBuilder(prototype);
  }

  /**
   * <pre>
   * For reconnect interop test only.
   * Client tells server what reconnection parameters it used.
   * </pre>
   *
   * Protobuf type {@code grpc.testing.ReconnectParams}
   */
  public static final class Builder extends
      com.google.protobuf.GeneratedMessageLite.Builder<
        com.grpc.testing.ReconnectParams, Builder> implements
      // @@protoc_insertion_point(builder_implements:grpc.testing.ReconnectParams)
      com.grpc.testing.ReconnectParamsOrBuilder {
    // Construct using com.grpc.testing.ReconnectParams.newBuilder()
    private Builder() {
      super(DEFAULT_INSTANCE);
    }


    /**
     * <code>int32 max_reconnect_backoff_ms = 1 [json_name = "maxReconnectBackoffMs"];</code>
     * @return The maxReconnectBackoffMs.
     */
    @java.lang.Override
    public int getMaxReconnectBackoffMs() {
      return instance.getMaxReconnectBackoffMs();
    }
    /**
     * <code>int32 max_reconnect_backoff_ms = 1 [json_name = "maxReconnectBackoffMs"];</code>
     * @param value The maxReconnectBackoffMs to set.
     * @return This builder for chaining.
     */
    public Builder setMaxReconnectBackoffMs(int value) {
      copyOnWrite();
      instance.setMaxReconnectBackoffMs(value);
      return this;
    }
    /**
     * <code>int32 max_reconnect_backoff_ms = 1 [json_name = "maxReconnectBackoffMs"];</code>
     * @return This builder for chaining.
     */
    public Builder clearMaxReconnectBackoffMs() {
      copyOnWrite();
      instance.clearMaxReconnectBackoffMs();
      return this;
    }

    // @@protoc_insertion_point(builder_scope:grpc.testing.ReconnectParams)
  }
  @java.lang.Override
  @java.lang.SuppressWarnings({"unchecked", "fallthrough"})
  protected final java.lang.Object dynamicMethod(
      com.google.protobuf.GeneratedMessageLite.MethodToInvoke method,
      java.lang.Object arg0, java.lang.Object arg1) {
    switch (method) {
      case NEW_MUTABLE_INSTANCE: {
        return new com.grpc.testing.ReconnectParams();
      }
      case NEW_BUILDER: {
        return new Builder();
      }
      case BUILD_MESSAGE_INFO: {
          java.lang.Object[] objects = new java.lang.Object[] {
            "maxReconnectBackoffMs_",
          };
          java.lang.String info =
              "\u0000\u0001\u0000\u0000\u0001\u0001\u0001\u0000\u0000\u0000\u0001\u0004";
          return newMessageInfo(DEFAULT_INSTANCE, info, objects);
      }
      // fall through
      case GET_DEFAULT_INSTANCE: {
        return DEFAULT_INSTANCE;
      }
      case GET_PARSER: {
        com.google.protobuf.Parser<com.grpc.testing.ReconnectParams> parser = PARSER;
        if (parser == null) {
          synchronized (com.grpc.testing.ReconnectParams.class) {
            parser = PARSER;
            if (parser == null) {
              parser =
                  new DefaultInstanceBasedParser<com.grpc.testing.ReconnectParams>(
                      DEFAULT_INSTANCE);
              PARSER = parser;
            }
          }
        }
        return parser;
    }
    case GET_MEMOIZED_IS_INITIALIZED: {
      return (byte) 1;
    }
    case SET_MEMOIZED_IS_INITIALIZED: {
      return null;
    }
    }
    throw new UnsupportedOperationException();
  }


  // @@protoc_insertion_point(class_scope:grpc.testing.ReconnectParams)
  private static final com.grpc.testing.ReconnectParams DEFAULT_INSTANCE;
  static {
    ReconnectParams defaultInstance = new ReconnectParams();
    // New instances are implicitly immutable so no need to make
    // immutable.
    DEFAULT_INSTANCE = defaultInstance;
    com.google.protobuf.GeneratedMessageLite.registerDefaultInstance(
      ReconnectParams.class, defaultInstance);
  }

  public static com.grpc.testing.ReconnectParams getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static volatile com.google.protobuf.Parser<ReconnectParams> PARSER;

  public static com.google.protobuf.Parser<ReconnectParams> parser() {
    return DEFAULT_INSTANCE.getParserForType();
  }
}


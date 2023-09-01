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
 * TODO(dgq): Go back to using well-known types once
 * https://github.com/grpc/grpc/issues/6980 has been fixed.
 * import "google/protobuf/wrappers.proto";
 * </pre>
 *
 * Protobuf type {@code grpc.testing.BoolValue}
 */
public  final class BoolValue extends
    com.google.protobuf.GeneratedMessageLite<
        BoolValue, BoolValue.Builder> implements
    // @@protoc_insertion_point(message_implements:grpc.testing.BoolValue)
    BoolValueOrBuilder {
  private BoolValue() {
  }
  public static final int VALUE_FIELD_NUMBER = 1;
  private boolean value_;
  /**
   * <pre>
   * The bool value.
   * </pre>
   *
   * <code>bool value = 1 [json_name = "value"];</code>
   * @return The value.
   */
  @java.lang.Override
  public boolean getValue() {
    return value_;
  }
  /**
   * <pre>
   * The bool value.
   * </pre>
   *
   * <code>bool value = 1 [json_name = "value"];</code>
   * @param value The value to set.
   */
  private void setValue(boolean value) {
    
    value_ = value;
  }
  /**
   * <pre>
   * The bool value.
   * </pre>
   *
   * <code>bool value = 1 [json_name = "value"];</code>
   */
  private void clearValue() {
    
    value_ = false;
  }

  public static com.grpc.testing.BoolValue parseFrom(
      java.nio.ByteBuffer data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, data);
  }
  public static com.grpc.testing.BoolValue parseFrom(
      java.nio.ByteBuffer data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, data, extensionRegistry);
  }
  public static com.grpc.testing.BoolValue parseFrom(
      com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, data);
  }
  public static com.grpc.testing.BoolValue parseFrom(
      com.google.protobuf.ByteString data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, data, extensionRegistry);
  }
  public static com.grpc.testing.BoolValue parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, data);
  }
  public static com.grpc.testing.BoolValue parseFrom(
      byte[] data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, data, extensionRegistry);
  }
  public static com.grpc.testing.BoolValue parseFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, input);
  }
  public static com.grpc.testing.BoolValue parseFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, input, extensionRegistry);
  }
  public static com.grpc.testing.BoolValue parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    return parseDelimitedFrom(DEFAULT_INSTANCE, input);
  }
  public static com.grpc.testing.BoolValue parseDelimitedFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return parseDelimitedFrom(DEFAULT_INSTANCE, input, extensionRegistry);
  }
  public static com.grpc.testing.BoolValue parseFrom(
      com.google.protobuf.CodedInputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, input);
  }
  public static com.grpc.testing.BoolValue parseFrom(
      com.google.protobuf.CodedInputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, input, extensionRegistry);
  }

  public static Builder newBuilder() {
    return (Builder) DEFAULT_INSTANCE.createBuilder();
  }
  public static Builder newBuilder(com.grpc.testing.BoolValue prototype) {
    return (Builder) DEFAULT_INSTANCE.createBuilder(prototype);
  }

  /**
   * <pre>
   * TODO(dgq): Go back to using well-known types once
   * https://github.com/grpc/grpc/issues/6980 has been fixed.
   * import "google/protobuf/wrappers.proto";
   * </pre>
   *
   * Protobuf type {@code grpc.testing.BoolValue}
   */
  public static final class Builder extends
      com.google.protobuf.GeneratedMessageLite.Builder<
        com.grpc.testing.BoolValue, Builder> implements
      // @@protoc_insertion_point(builder_implements:grpc.testing.BoolValue)
      com.grpc.testing.BoolValueOrBuilder {
    // Construct using com.grpc.testing.BoolValue.newBuilder()
    private Builder() {
      super(DEFAULT_INSTANCE);
    }


    /**
     * <pre>
     * The bool value.
     * </pre>
     *
     * <code>bool value = 1 [json_name = "value"];</code>
     * @return The value.
     */
    @java.lang.Override
    public boolean getValue() {
      return instance.getValue();
    }
    /**
     * <pre>
     * The bool value.
     * </pre>
     *
     * <code>bool value = 1 [json_name = "value"];</code>
     * @param value The value to set.
     * @return This builder for chaining.
     */
    public Builder setValue(boolean value) {
      copyOnWrite();
      instance.setValue(value);
      return this;
    }
    /**
     * <pre>
     * The bool value.
     * </pre>
     *
     * <code>bool value = 1 [json_name = "value"];</code>
     * @return This builder for chaining.
     */
    public Builder clearValue() {
      copyOnWrite();
      instance.clearValue();
      return this;
    }

    // @@protoc_insertion_point(builder_scope:grpc.testing.BoolValue)
  }
  @java.lang.Override
  @java.lang.SuppressWarnings({"unchecked", "fallthrough"})
  protected final java.lang.Object dynamicMethod(
      com.google.protobuf.GeneratedMessageLite.MethodToInvoke method,
      java.lang.Object arg0, java.lang.Object arg1) {
    switch (method) {
      case NEW_MUTABLE_INSTANCE: {
        return new com.grpc.testing.BoolValue();
      }
      case NEW_BUILDER: {
        return new Builder();
      }
      case BUILD_MESSAGE_INFO: {
          java.lang.Object[] objects = new java.lang.Object[] {
            "value_",
          };
          java.lang.String info =
              "\u0000\u0001\u0000\u0000\u0001\u0001\u0001\u0000\u0000\u0000\u0001\u0007";
          return newMessageInfo(DEFAULT_INSTANCE, info, objects);
      }
      // fall through
      case GET_DEFAULT_INSTANCE: {
        return DEFAULT_INSTANCE;
      }
      case GET_PARSER: {
        com.google.protobuf.Parser<com.grpc.testing.BoolValue> parser = PARSER;
        if (parser == null) {
          synchronized (com.grpc.testing.BoolValue.class) {
            parser = PARSER;
            if (parser == null) {
              parser =
                  new DefaultInstanceBasedParser<com.grpc.testing.BoolValue>(
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


  // @@protoc_insertion_point(class_scope:grpc.testing.BoolValue)
  private static final com.grpc.testing.BoolValue DEFAULT_INSTANCE;
  static {
    BoolValue defaultInstance = new BoolValue();
    // New instances are implicitly immutable so no need to make
    // immutable.
    DEFAULT_INSTANCE = defaultInstance;
    com.google.protobuf.GeneratedMessageLite.registerDefaultInstance(
      BoolValue.class, defaultInstance);
  }

  public static com.grpc.testing.BoolValue getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static volatile com.google.protobuf.Parser<BoolValue> PARSER;

  public static com.google.protobuf.Parser<BoolValue> parser() {
    return DEFAULT_INSTANCE.getParserForType();
  }
}


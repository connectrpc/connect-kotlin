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
// source: buf/javamultiplefiles/enabled/v1/enabled_empty.proto

package com.buf.javamultiplefiles.enabled.v1;

/**
 * Protobuf type {@code buf.javamultiplefiles.enabled.v1.EnabledEmpty}
 */
public  final class EnabledEmpty extends
    com.google.protobuf.GeneratedMessageLite<
        EnabledEmpty, EnabledEmpty.Builder> implements
    // @@protoc_insertion_point(message_implements:buf.javamultiplefiles.enabled.v1.EnabledEmpty)
    EnabledEmptyOrBuilder {
  private EnabledEmpty() {
  }
  public static com.buf.javamultiplefiles.enabled.v1.EnabledEmpty parseFrom(
      java.nio.ByteBuffer data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, data);
  }
  public static com.buf.javamultiplefiles.enabled.v1.EnabledEmpty parseFrom(
      java.nio.ByteBuffer data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, data, extensionRegistry);
  }
  public static com.buf.javamultiplefiles.enabled.v1.EnabledEmpty parseFrom(
      com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, data);
  }
  public static com.buf.javamultiplefiles.enabled.v1.EnabledEmpty parseFrom(
      com.google.protobuf.ByteString data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, data, extensionRegistry);
  }
  public static com.buf.javamultiplefiles.enabled.v1.EnabledEmpty parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, data);
  }
  public static com.buf.javamultiplefiles.enabled.v1.EnabledEmpty parseFrom(
      byte[] data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, data, extensionRegistry);
  }
  public static com.buf.javamultiplefiles.enabled.v1.EnabledEmpty parseFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, input);
  }
  public static com.buf.javamultiplefiles.enabled.v1.EnabledEmpty parseFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, input, extensionRegistry);
  }
  public static com.buf.javamultiplefiles.enabled.v1.EnabledEmpty parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    return parseDelimitedFrom(DEFAULT_INSTANCE, input);
  }
  public static com.buf.javamultiplefiles.enabled.v1.EnabledEmpty parseDelimitedFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return parseDelimitedFrom(DEFAULT_INSTANCE, input, extensionRegistry);
  }
  public static com.buf.javamultiplefiles.enabled.v1.EnabledEmpty parseFrom(
      com.google.protobuf.CodedInputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, input);
  }
  public static com.buf.javamultiplefiles.enabled.v1.EnabledEmpty parseFrom(
      com.google.protobuf.CodedInputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, input, extensionRegistry);
  }

  public static Builder newBuilder() {
    return (Builder) DEFAULT_INSTANCE.createBuilder();
  }
  public static Builder newBuilder(com.buf.javamultiplefiles.enabled.v1.EnabledEmpty prototype) {
    return (Builder) DEFAULT_INSTANCE.createBuilder(prototype);
  }

  /**
   * Protobuf type {@code buf.javamultiplefiles.enabled.v1.EnabledEmpty}
   */
  public static final class Builder extends
      com.google.protobuf.GeneratedMessageLite.Builder<
        com.buf.javamultiplefiles.enabled.v1.EnabledEmpty, Builder> implements
      // @@protoc_insertion_point(builder_implements:buf.javamultiplefiles.enabled.v1.EnabledEmpty)
      com.buf.javamultiplefiles.enabled.v1.EnabledEmptyOrBuilder {
    // Construct using com.buf.javamultiplefiles.enabled.v1.EnabledEmpty.newBuilder()
    private Builder() {
      super(DEFAULT_INSTANCE);
    }


    // @@protoc_insertion_point(builder_scope:buf.javamultiplefiles.enabled.v1.EnabledEmpty)
  }
  @java.lang.Override
  @java.lang.SuppressWarnings({"unchecked", "fallthrough"})
  protected final java.lang.Object dynamicMethod(
      com.google.protobuf.GeneratedMessageLite.MethodToInvoke method,
      java.lang.Object arg0, java.lang.Object arg1) {
    switch (method) {
      case NEW_MUTABLE_INSTANCE: {
        return new com.buf.javamultiplefiles.enabled.v1.EnabledEmpty();
      }
      case NEW_BUILDER: {
        return new Builder();
      }
      case BUILD_MESSAGE_INFO: {
          java.lang.Object[] objects = null;java.lang.String info =
              "\u0000\u0000";
          return newMessageInfo(DEFAULT_INSTANCE, info, objects);
      }
      // fall through
      case GET_DEFAULT_INSTANCE: {
        return DEFAULT_INSTANCE;
      }
      case GET_PARSER: {
        com.google.protobuf.Parser<com.buf.javamultiplefiles.enabled.v1.EnabledEmpty> parser = PARSER;
        if (parser == null) {
          synchronized (com.buf.javamultiplefiles.enabled.v1.EnabledEmpty.class) {
            parser = PARSER;
            if (parser == null) {
              parser =
                  new DefaultInstanceBasedParser<com.buf.javamultiplefiles.enabled.v1.EnabledEmpty>(
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


  // @@protoc_insertion_point(class_scope:buf.javamultiplefiles.enabled.v1.EnabledEmpty)
  private static final com.buf.javamultiplefiles.enabled.v1.EnabledEmpty DEFAULT_INSTANCE;
  static {
    EnabledEmpty defaultInstance = new EnabledEmpty();
    // New instances are implicitly immutable so no need to make
    // immutable.
    DEFAULT_INSTANCE = defaultInstance;
    com.google.protobuf.GeneratedMessageLite.registerDefaultInstance(
      EnabledEmpty.class, defaultInstance);
  }

  public static com.buf.javamultiplefiles.enabled.v1.EnabledEmpty getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static volatile com.google.protobuf.Parser<EnabledEmpty> PARSER;

  public static com.google.protobuf.Parser<EnabledEmpty> parser() {
    return DEFAULT_INSTANCE.getParserForType();
  }
}


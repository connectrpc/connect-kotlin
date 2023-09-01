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
// source: buf/javamultiplefiles/unspecified/v1/unspecified.proto

package com.buf.javamultiplefiles.unspecified.v1;

/**
 * Protobuf type {@code buf.javamultiplefiles.unspecified.v1.UnspecifiedRequest}
 */
public  final class UnspecifiedRequest extends
    com.google.protobuf.GeneratedMessageLite<
        UnspecifiedRequest, UnspecifiedRequest.Builder> implements
    // @@protoc_insertion_point(message_implements:buf.javamultiplefiles.unspecified.v1.UnspecifiedRequest)
    UnspecifiedRequestOrBuilder {
  private UnspecifiedRequest() {
    sentence_ = "";
  }
  public static final int SENTENCE_FIELD_NUMBER = 1;
  private java.lang.String sentence_;
  /**
   * <code>string sentence = 1 [json_name = "sentence"];</code>
   * @return The sentence.
   */
  @java.lang.Override
  public java.lang.String getSentence() {
    return sentence_;
  }
  /**
   * <code>string sentence = 1 [json_name = "sentence"];</code>
   * @return The bytes for sentence.
   */
  @java.lang.Override
  public com.google.protobuf.ByteString
      getSentenceBytes() {
    return com.google.protobuf.ByteString.copyFromUtf8(sentence_);
  }
  /**
   * <code>string sentence = 1 [json_name = "sentence"];</code>
   * @param value The sentence to set.
   */
  private void setSentence(
      java.lang.String value) {
    java.lang.Class<?> valueClass = value.getClass();
  
    sentence_ = value;
  }
  /**
   * <code>string sentence = 1 [json_name = "sentence"];</code>
   */
  private void clearSentence() {
    
    sentence_ = getDefaultInstance().getSentence();
  }
  /**
   * <code>string sentence = 1 [json_name = "sentence"];</code>
   * @param value The bytes for sentence to set.
   */
  private void setSentenceBytes(
      com.google.protobuf.ByteString value) {
    checkByteStringIsUtf8(value);
    sentence_ = value.toStringUtf8();
    
  }

  public static com.buf.javamultiplefiles.unspecified.v1.UnspecifiedRequest parseFrom(
      java.nio.ByteBuffer data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, data);
  }
  public static com.buf.javamultiplefiles.unspecified.v1.UnspecifiedRequest parseFrom(
      java.nio.ByteBuffer data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, data, extensionRegistry);
  }
  public static com.buf.javamultiplefiles.unspecified.v1.UnspecifiedRequest parseFrom(
      com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, data);
  }
  public static com.buf.javamultiplefiles.unspecified.v1.UnspecifiedRequest parseFrom(
      com.google.protobuf.ByteString data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, data, extensionRegistry);
  }
  public static com.buf.javamultiplefiles.unspecified.v1.UnspecifiedRequest parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, data);
  }
  public static com.buf.javamultiplefiles.unspecified.v1.UnspecifiedRequest parseFrom(
      byte[] data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, data, extensionRegistry);
  }
  public static com.buf.javamultiplefiles.unspecified.v1.UnspecifiedRequest parseFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, input);
  }
  public static com.buf.javamultiplefiles.unspecified.v1.UnspecifiedRequest parseFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, input, extensionRegistry);
  }
  public static com.buf.javamultiplefiles.unspecified.v1.UnspecifiedRequest parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    return parseDelimitedFrom(DEFAULT_INSTANCE, input);
  }
  public static com.buf.javamultiplefiles.unspecified.v1.UnspecifiedRequest parseDelimitedFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return parseDelimitedFrom(DEFAULT_INSTANCE, input, extensionRegistry);
  }
  public static com.buf.javamultiplefiles.unspecified.v1.UnspecifiedRequest parseFrom(
      com.google.protobuf.CodedInputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, input);
  }
  public static com.buf.javamultiplefiles.unspecified.v1.UnspecifiedRequest parseFrom(
      com.google.protobuf.CodedInputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, input, extensionRegistry);
  }

  public static Builder newBuilder() {
    return (Builder) DEFAULT_INSTANCE.createBuilder();
  }
  public static Builder newBuilder(com.buf.javamultiplefiles.unspecified.v1.UnspecifiedRequest prototype) {
    return (Builder) DEFAULT_INSTANCE.createBuilder(prototype);
  }

  /**
   * Protobuf type {@code buf.javamultiplefiles.unspecified.v1.UnspecifiedRequest}
   */
  public static final class Builder extends
      com.google.protobuf.GeneratedMessageLite.Builder<
        com.buf.javamultiplefiles.unspecified.v1.UnspecifiedRequest, Builder> implements
      // @@protoc_insertion_point(builder_implements:buf.javamultiplefiles.unspecified.v1.UnspecifiedRequest)
      com.buf.javamultiplefiles.unspecified.v1.UnspecifiedRequestOrBuilder {
    // Construct using com.buf.javamultiplefiles.unspecified.v1.UnspecifiedRequest.newBuilder()
    private Builder() {
      super(DEFAULT_INSTANCE);
    }


    /**
     * <code>string sentence = 1 [json_name = "sentence"];</code>
     * @return The sentence.
     */
    @java.lang.Override
    public java.lang.String getSentence() {
      return instance.getSentence();
    }
    /**
     * <code>string sentence = 1 [json_name = "sentence"];</code>
     * @return The bytes for sentence.
     */
    @java.lang.Override
    public com.google.protobuf.ByteString
        getSentenceBytes() {
      return instance.getSentenceBytes();
    }
    /**
     * <code>string sentence = 1 [json_name = "sentence"];</code>
     * @param value The sentence to set.
     * @return This builder for chaining.
     */
    public Builder setSentence(
        java.lang.String value) {
      copyOnWrite();
      instance.setSentence(value);
      return this;
    }
    /**
     * <code>string sentence = 1 [json_name = "sentence"];</code>
     * @return This builder for chaining.
     */
    public Builder clearSentence() {
      copyOnWrite();
      instance.clearSentence();
      return this;
    }
    /**
     * <code>string sentence = 1 [json_name = "sentence"];</code>
     * @param value The bytes for sentence to set.
     * @return This builder for chaining.
     */
    public Builder setSentenceBytes(
        com.google.protobuf.ByteString value) {
      copyOnWrite();
      instance.setSentenceBytes(value);
      return this;
    }

    // @@protoc_insertion_point(builder_scope:buf.javamultiplefiles.unspecified.v1.UnspecifiedRequest)
  }
  @java.lang.Override
  @java.lang.SuppressWarnings({"unchecked", "fallthrough"})
  protected final java.lang.Object dynamicMethod(
      com.google.protobuf.GeneratedMessageLite.MethodToInvoke method,
      java.lang.Object arg0, java.lang.Object arg1) {
    switch (method) {
      case NEW_MUTABLE_INSTANCE: {
        return new com.buf.javamultiplefiles.unspecified.v1.UnspecifiedRequest();
      }
      case NEW_BUILDER: {
        return new Builder();
      }
      case BUILD_MESSAGE_INFO: {
          java.lang.Object[] objects = new java.lang.Object[] {
            "sentence_",
          };
          java.lang.String info =
              "\u0000\u0001\u0000\u0000\u0001\u0001\u0001\u0000\u0000\u0000\u0001\u0208";
          return newMessageInfo(DEFAULT_INSTANCE, info, objects);
      }
      // fall through
      case GET_DEFAULT_INSTANCE: {
        return DEFAULT_INSTANCE;
      }
      case GET_PARSER: {
        com.google.protobuf.Parser<com.buf.javamultiplefiles.unspecified.v1.UnspecifiedRequest> parser = PARSER;
        if (parser == null) {
          synchronized (com.buf.javamultiplefiles.unspecified.v1.UnspecifiedRequest.class) {
            parser = PARSER;
            if (parser == null) {
              parser =
                  new DefaultInstanceBasedParser<com.buf.javamultiplefiles.unspecified.v1.UnspecifiedRequest>(
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


  // @@protoc_insertion_point(class_scope:buf.javamultiplefiles.unspecified.v1.UnspecifiedRequest)
  private static final com.buf.javamultiplefiles.unspecified.v1.UnspecifiedRequest DEFAULT_INSTANCE;
  static {
    UnspecifiedRequest defaultInstance = new UnspecifiedRequest();
    // New instances are implicitly immutable so no need to make
    // immutable.
    DEFAULT_INSTANCE = defaultInstance;
    com.google.protobuf.GeneratedMessageLite.registerDefaultInstance(
      UnspecifiedRequest.class, defaultInstance);
  }

  public static com.buf.javamultiplefiles.unspecified.v1.UnspecifiedRequest getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static volatile com.google.protobuf.Parser<UnspecifiedRequest> PARSER;

  public static com.google.protobuf.Parser<UnspecifiedRequest> parser() {
    return DEFAULT_INSTANCE.getParserForType();
  }
}


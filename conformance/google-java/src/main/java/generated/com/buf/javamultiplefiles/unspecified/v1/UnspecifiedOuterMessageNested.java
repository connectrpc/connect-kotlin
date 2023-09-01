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
// source: buf/javamultiplefiles/unspecified/v1/enabled_nested.proto

package com.buf.javamultiplefiles.unspecified.v1;

/**
 * Protobuf type {@code buf.javamultiplefiles.unspecified.v1.UnspecifiedOuterMessageNested}
 */
public final class UnspecifiedOuterMessageNested extends
    com.google.protobuf.GeneratedMessageV3 implements
    // @@protoc_insertion_point(message_implements:buf.javamultiplefiles.unspecified.v1.UnspecifiedOuterMessageNested)
    UnspecifiedOuterMessageNestedOrBuilder {
private static final long serialVersionUID = 0L;
  // Use UnspecifiedOuterMessageNested.newBuilder() to construct.
  private UnspecifiedOuterMessageNested(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
    super(builder);
  }
  private UnspecifiedOuterMessageNested() {
  }

  @java.lang.Override
  @SuppressWarnings({"unused"})
  protected java.lang.Object newInstance(
      UnusedPrivateParameter unused) {
    return new UnspecifiedOuterMessageNested();
  }

  @java.lang.Override
  public final com.google.protobuf.UnknownFieldSet
  getUnknownFields() {
    return this.unknownFields;
  }
  private UnspecifiedOuterMessageNested(
      com.google.protobuf.CodedInputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    this();
    if (extensionRegistry == null) {
      throw new java.lang.NullPointerException();
    }
    com.google.protobuf.UnknownFieldSet.Builder unknownFields =
        com.google.protobuf.UnknownFieldSet.newBuilder();
    try {
      boolean done = false;
      while (!done) {
        int tag = input.readTag();
        switch (tag) {
          case 0:
            done = true;
            break;
          default: {
            if (!parseUnknownField(
                input, unknownFields, extensionRegistry, tag)) {
              done = true;
            }
            break;
          }
        }
      }
    } catch (com.google.protobuf.InvalidProtocolBufferException e) {
      throw e.setUnfinishedMessage(this);
    } catch (com.google.protobuf.UninitializedMessageException e) {
      throw e.asInvalidProtocolBufferException().setUnfinishedMessage(this);
    } catch (java.io.IOException e) {
      throw new com.google.protobuf.InvalidProtocolBufferException(
          e).setUnfinishedMessage(this);
    } finally {
      this.unknownFields = unknownFields.build();
      makeExtensionsImmutable();
    }
  }
  public static final com.google.protobuf.Descriptors.Descriptor
      getDescriptor() {
    return com.buf.javamultiplefiles.unspecified.v1.EnabledNestedProto.internal_static_buf_javamultiplefiles_unspecified_v1_UnspecifiedOuterMessageNested_descriptor;
  }

  @java.lang.Override
  protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internalGetFieldAccessorTable() {
    return com.buf.javamultiplefiles.unspecified.v1.EnabledNestedProto.internal_static_buf_javamultiplefiles_unspecified_v1_UnspecifiedOuterMessageNested_fieldAccessorTable
        .ensureFieldAccessorsInitialized(
            com.buf.javamultiplefiles.unspecified.v1.UnspecifiedOuterMessageNested.class, com.buf.javamultiplefiles.unspecified.v1.UnspecifiedOuterMessageNested.Builder.class);
  }

  public interface InnerMessageOrBuilder extends
      // @@protoc_insertion_point(interface_extends:buf.javamultiplefiles.unspecified.v1.UnspecifiedOuterMessageNested.InnerMessage)
      com.google.protobuf.MessageOrBuilder {
  }
  /**
   * Protobuf type {@code buf.javamultiplefiles.unspecified.v1.UnspecifiedOuterMessageNested.InnerMessage}
   */
  public static final class InnerMessage extends
      com.google.protobuf.GeneratedMessageV3 implements
      // @@protoc_insertion_point(message_implements:buf.javamultiplefiles.unspecified.v1.UnspecifiedOuterMessageNested.InnerMessage)
      InnerMessageOrBuilder {
  private static final long serialVersionUID = 0L;
    // Use InnerMessage.newBuilder() to construct.
    private InnerMessage(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
      super(builder);
    }
    private InnerMessage() {
    }

    @java.lang.Override
    @SuppressWarnings({"unused"})
    protected java.lang.Object newInstance(
        UnusedPrivateParameter unused) {
      return new InnerMessage();
    }

    @java.lang.Override
    public final com.google.protobuf.UnknownFieldSet
    getUnknownFields() {
      return this.unknownFields;
    }
    private InnerMessage(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      this();
      if (extensionRegistry == null) {
        throw new java.lang.NullPointerException();
      }
      com.google.protobuf.UnknownFieldSet.Builder unknownFields =
          com.google.protobuf.UnknownFieldSet.newBuilder();
      try {
        boolean done = false;
        while (!done) {
          int tag = input.readTag();
          switch (tag) {
            case 0:
              done = true;
              break;
            default: {
              if (!parseUnknownField(
                  input, unknownFields, extensionRegistry, tag)) {
                done = true;
              }
              break;
            }
          }
        }
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        throw e.setUnfinishedMessage(this);
      } catch (com.google.protobuf.UninitializedMessageException e) {
        throw e.asInvalidProtocolBufferException().setUnfinishedMessage(this);
      } catch (java.io.IOException e) {
        throw new com.google.protobuf.InvalidProtocolBufferException(
            e).setUnfinishedMessage(this);
      } finally {
        this.unknownFields = unknownFields.build();
        makeExtensionsImmutable();
      }
    }
    public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
      return com.buf.javamultiplefiles.unspecified.v1.EnabledNestedProto.internal_static_buf_javamultiplefiles_unspecified_v1_UnspecifiedOuterMessageNested_InnerMessage_descriptor;
    }

    @java.lang.Override
    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return com.buf.javamultiplefiles.unspecified.v1.EnabledNestedProto.internal_static_buf_javamultiplefiles_unspecified_v1_UnspecifiedOuterMessageNested_InnerMessage_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              com.buf.javamultiplefiles.unspecified.v1.UnspecifiedOuterMessageNested.InnerMessage.class, com.buf.javamultiplefiles.unspecified.v1.UnspecifiedOuterMessageNested.InnerMessage.Builder.class);
    }

    private byte memoizedIsInitialized = -1;
    @java.lang.Override
    public final boolean isInitialized() {
      byte isInitialized = memoizedIsInitialized;
      if (isInitialized == 1) return true;
      if (isInitialized == 0) return false;

      memoizedIsInitialized = 1;
      return true;
    }

    @java.lang.Override
    public void writeTo(com.google.protobuf.CodedOutputStream output)
                        throws java.io.IOException {
      unknownFields.writeTo(output);
    }

    @java.lang.Override
    public int getSerializedSize() {
      int size = memoizedSize;
      if (size != -1) return size;

      size = 0;
      size += unknownFields.getSerializedSize();
      memoizedSize = size;
      return size;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object obj) {
      if (obj == this) {
       return true;
      }
      if (!(obj instanceof com.buf.javamultiplefiles.unspecified.v1.UnspecifiedOuterMessageNested.InnerMessage)) {
        return super.equals(obj);
      }
      com.buf.javamultiplefiles.unspecified.v1.UnspecifiedOuterMessageNested.InnerMessage other = (com.buf.javamultiplefiles.unspecified.v1.UnspecifiedOuterMessageNested.InnerMessage) obj;

      if (!unknownFields.equals(other.unknownFields)) return false;
      return true;
    }

    @java.lang.Override
    public int hashCode() {
      if (memoizedHashCode != 0) {
        return memoizedHashCode;
      }
      int hash = 41;
      hash = (19 * hash) + getDescriptor().hashCode();
      hash = (29 * hash) + unknownFields.hashCode();
      memoizedHashCode = hash;
      return hash;
    }

    public static com.buf.javamultiplefiles.unspecified.v1.UnspecifiedOuterMessageNested.InnerMessage parseFrom(
        java.nio.ByteBuffer data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }
    public static com.buf.javamultiplefiles.unspecified.v1.UnspecifiedOuterMessageNested.InnerMessage parseFrom(
        java.nio.ByteBuffer data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }
    public static com.buf.javamultiplefiles.unspecified.v1.UnspecifiedOuterMessageNested.InnerMessage parseFrom(
        com.google.protobuf.ByteString data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }
    public static com.buf.javamultiplefiles.unspecified.v1.UnspecifiedOuterMessageNested.InnerMessage parseFrom(
        com.google.protobuf.ByteString data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }
    public static com.buf.javamultiplefiles.unspecified.v1.UnspecifiedOuterMessageNested.InnerMessage parseFrom(byte[] data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }
    public static com.buf.javamultiplefiles.unspecified.v1.UnspecifiedOuterMessageNested.InnerMessage parseFrom(
        byte[] data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }
    public static com.buf.javamultiplefiles.unspecified.v1.UnspecifiedOuterMessageNested.InnerMessage parseFrom(java.io.InputStream input)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
          .parseWithIOException(PARSER, input);
    }
    public static com.buf.javamultiplefiles.unspecified.v1.UnspecifiedOuterMessageNested.InnerMessage parseFrom(
        java.io.InputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
          .parseWithIOException(PARSER, input, extensionRegistry);
    }
    public static com.buf.javamultiplefiles.unspecified.v1.UnspecifiedOuterMessageNested.InnerMessage parseDelimitedFrom(java.io.InputStream input)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
          .parseDelimitedWithIOException(PARSER, input);
    }
    public static com.buf.javamultiplefiles.unspecified.v1.UnspecifiedOuterMessageNested.InnerMessage parseDelimitedFrom(
        java.io.InputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
          .parseDelimitedWithIOException(PARSER, input, extensionRegistry);
    }
    public static com.buf.javamultiplefiles.unspecified.v1.UnspecifiedOuterMessageNested.InnerMessage parseFrom(
        com.google.protobuf.CodedInputStream input)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
          .parseWithIOException(PARSER, input);
    }
    public static com.buf.javamultiplefiles.unspecified.v1.UnspecifiedOuterMessageNested.InnerMessage parseFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
          .parseWithIOException(PARSER, input, extensionRegistry);
    }

    @java.lang.Override
    public Builder newBuilderForType() { return newBuilder(); }
    public static Builder newBuilder() {
      return DEFAULT_INSTANCE.toBuilder();
    }
    public static Builder newBuilder(com.buf.javamultiplefiles.unspecified.v1.UnspecifiedOuterMessageNested.InnerMessage prototype) {
      return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
    }
    @java.lang.Override
    public Builder toBuilder() {
      return this == DEFAULT_INSTANCE
          ? new Builder() : new Builder().mergeFrom(this);
    }

    @java.lang.Override
    protected Builder newBuilderForType(
        com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
      Builder builder = new Builder(parent);
      return builder;
    }
    /**
     * Protobuf type {@code buf.javamultiplefiles.unspecified.v1.UnspecifiedOuterMessageNested.InnerMessage}
     */
    public static final class Builder extends
        com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
        // @@protoc_insertion_point(builder_implements:buf.javamultiplefiles.unspecified.v1.UnspecifiedOuterMessageNested.InnerMessage)
        com.buf.javamultiplefiles.unspecified.v1.UnspecifiedOuterMessageNested.InnerMessageOrBuilder {
      public static final com.google.protobuf.Descriptors.Descriptor
          getDescriptor() {
        return com.buf.javamultiplefiles.unspecified.v1.EnabledNestedProto.internal_static_buf_javamultiplefiles_unspecified_v1_UnspecifiedOuterMessageNested_InnerMessage_descriptor;
      }

      @java.lang.Override
      protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
          internalGetFieldAccessorTable() {
        return com.buf.javamultiplefiles.unspecified.v1.EnabledNestedProto.internal_static_buf_javamultiplefiles_unspecified_v1_UnspecifiedOuterMessageNested_InnerMessage_fieldAccessorTable
            .ensureFieldAccessorsInitialized(
                com.buf.javamultiplefiles.unspecified.v1.UnspecifiedOuterMessageNested.InnerMessage.class, com.buf.javamultiplefiles.unspecified.v1.UnspecifiedOuterMessageNested.InnerMessage.Builder.class);
      }

      // Construct using com.buf.javamultiplefiles.unspecified.v1.UnspecifiedOuterMessageNested.InnerMessage.newBuilder()
      private Builder() {
        maybeForceBuilderInitialization();
      }

      private Builder(
          com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
        super(parent);
        maybeForceBuilderInitialization();
      }
      private void maybeForceBuilderInitialization() {
        if (com.google.protobuf.GeneratedMessageV3
                .alwaysUseFieldBuilders) {
        }
      }
      @java.lang.Override
      public Builder clear() {
        super.clear();
        return this;
      }

      @java.lang.Override
      public com.google.protobuf.Descriptors.Descriptor
          getDescriptorForType() {
        return com.buf.javamultiplefiles.unspecified.v1.EnabledNestedProto.internal_static_buf_javamultiplefiles_unspecified_v1_UnspecifiedOuterMessageNested_InnerMessage_descriptor;
      }

      @java.lang.Override
      public com.buf.javamultiplefiles.unspecified.v1.UnspecifiedOuterMessageNested.InnerMessage getDefaultInstanceForType() {
        return com.buf.javamultiplefiles.unspecified.v1.UnspecifiedOuterMessageNested.InnerMessage.getDefaultInstance();
      }

      @java.lang.Override
      public com.buf.javamultiplefiles.unspecified.v1.UnspecifiedOuterMessageNested.InnerMessage build() {
        com.buf.javamultiplefiles.unspecified.v1.UnspecifiedOuterMessageNested.InnerMessage result = buildPartial();
        if (!result.isInitialized()) {
          throw newUninitializedMessageException(result);
        }
        return result;
      }

      @java.lang.Override
      public com.buf.javamultiplefiles.unspecified.v1.UnspecifiedOuterMessageNested.InnerMessage buildPartial() {
        com.buf.javamultiplefiles.unspecified.v1.UnspecifiedOuterMessageNested.InnerMessage result = new com.buf.javamultiplefiles.unspecified.v1.UnspecifiedOuterMessageNested.InnerMessage(this);
        onBuilt();
        return result;
      }

      @java.lang.Override
      public Builder clone() {
        return super.clone();
      }
      @java.lang.Override
      public Builder setField(
          com.google.protobuf.Descriptors.FieldDescriptor field,
          java.lang.Object value) {
        return super.setField(field, value);
      }
      @java.lang.Override
      public Builder clearField(
          com.google.protobuf.Descriptors.FieldDescriptor field) {
        return super.clearField(field);
      }
      @java.lang.Override
      public Builder clearOneof(
          com.google.protobuf.Descriptors.OneofDescriptor oneof) {
        return super.clearOneof(oneof);
      }
      @java.lang.Override
      public Builder setRepeatedField(
          com.google.protobuf.Descriptors.FieldDescriptor field,
          int index, java.lang.Object value) {
        return super.setRepeatedField(field, index, value);
      }
      @java.lang.Override
      public Builder addRepeatedField(
          com.google.protobuf.Descriptors.FieldDescriptor field,
          java.lang.Object value) {
        return super.addRepeatedField(field, value);
      }
      @java.lang.Override
      public Builder mergeFrom(com.google.protobuf.Message other) {
        if (other instanceof com.buf.javamultiplefiles.unspecified.v1.UnspecifiedOuterMessageNested.InnerMessage) {
          return mergeFrom((com.buf.javamultiplefiles.unspecified.v1.UnspecifiedOuterMessageNested.InnerMessage)other);
        } else {
          super.mergeFrom(other);
          return this;
        }
      }

      public Builder mergeFrom(com.buf.javamultiplefiles.unspecified.v1.UnspecifiedOuterMessageNested.InnerMessage other) {
        if (other == com.buf.javamultiplefiles.unspecified.v1.UnspecifiedOuterMessageNested.InnerMessage.getDefaultInstance()) return this;
        this.mergeUnknownFields(other.unknownFields);
        onChanged();
        return this;
      }

      @java.lang.Override
      public final boolean isInitialized() {
        return true;
      }

      @java.lang.Override
      public Builder mergeFrom(
          com.google.protobuf.CodedInputStream input,
          com.google.protobuf.ExtensionRegistryLite extensionRegistry)
          throws java.io.IOException {
        com.buf.javamultiplefiles.unspecified.v1.UnspecifiedOuterMessageNested.InnerMessage parsedMessage = null;
        try {
          parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
        } catch (com.google.protobuf.InvalidProtocolBufferException e) {
          parsedMessage = (com.buf.javamultiplefiles.unspecified.v1.UnspecifiedOuterMessageNested.InnerMessage) e.getUnfinishedMessage();
          throw e.unwrapIOException();
        } finally {
          if (parsedMessage != null) {
            mergeFrom(parsedMessage);
          }
        }
        return this;
      }
      @java.lang.Override
      public final Builder setUnknownFields(
          final com.google.protobuf.UnknownFieldSet unknownFields) {
        return super.setUnknownFields(unknownFields);
      }

      @java.lang.Override
      public final Builder mergeUnknownFields(
          final com.google.protobuf.UnknownFieldSet unknownFields) {
        return super.mergeUnknownFields(unknownFields);
      }


      // @@protoc_insertion_point(builder_scope:buf.javamultiplefiles.unspecified.v1.UnspecifiedOuterMessageNested.InnerMessage)
    }

    // @@protoc_insertion_point(class_scope:buf.javamultiplefiles.unspecified.v1.UnspecifiedOuterMessageNested.InnerMessage)
    private static final com.buf.javamultiplefiles.unspecified.v1.UnspecifiedOuterMessageNested.InnerMessage DEFAULT_INSTANCE;
    static {
      DEFAULT_INSTANCE = new com.buf.javamultiplefiles.unspecified.v1.UnspecifiedOuterMessageNested.InnerMessage();
    }

    public static com.buf.javamultiplefiles.unspecified.v1.UnspecifiedOuterMessageNested.InnerMessage getDefaultInstance() {
      return DEFAULT_INSTANCE;
    }

    private static final com.google.protobuf.Parser<InnerMessage>
        PARSER = new com.google.protobuf.AbstractParser<InnerMessage>() {
      @java.lang.Override
      public InnerMessage parsePartialFrom(
          com.google.protobuf.CodedInputStream input,
          com.google.protobuf.ExtensionRegistryLite extensionRegistry)
          throws com.google.protobuf.InvalidProtocolBufferException {
        return new InnerMessage(input, extensionRegistry);
      }
    };

    public static com.google.protobuf.Parser<InnerMessage> parser() {
      return PARSER;
    }

    @java.lang.Override
    public com.google.protobuf.Parser<InnerMessage> getParserForType() {
      return PARSER;
    }

    @java.lang.Override
    public com.buf.javamultiplefiles.unspecified.v1.UnspecifiedOuterMessageNested.InnerMessage getDefaultInstanceForType() {
      return DEFAULT_INSTANCE;
    }

  }

  private byte memoizedIsInitialized = -1;
  @java.lang.Override
  public final boolean isInitialized() {
    byte isInitialized = memoizedIsInitialized;
    if (isInitialized == 1) return true;
    if (isInitialized == 0) return false;

    memoizedIsInitialized = 1;
    return true;
  }

  @java.lang.Override
  public void writeTo(com.google.protobuf.CodedOutputStream output)
                      throws java.io.IOException {
    unknownFields.writeTo(output);
  }

  @java.lang.Override
  public int getSerializedSize() {
    int size = memoizedSize;
    if (size != -1) return size;

    size = 0;
    size += unknownFields.getSerializedSize();
    memoizedSize = size;
    return size;
  }

  @java.lang.Override
  public boolean equals(final java.lang.Object obj) {
    if (obj == this) {
     return true;
    }
    if (!(obj instanceof com.buf.javamultiplefiles.unspecified.v1.UnspecifiedOuterMessageNested)) {
      return super.equals(obj);
    }
    com.buf.javamultiplefiles.unspecified.v1.UnspecifiedOuterMessageNested other = (com.buf.javamultiplefiles.unspecified.v1.UnspecifiedOuterMessageNested) obj;

    if (!unknownFields.equals(other.unknownFields)) return false;
    return true;
  }

  @java.lang.Override
  public int hashCode() {
    if (memoizedHashCode != 0) {
      return memoizedHashCode;
    }
    int hash = 41;
    hash = (19 * hash) + getDescriptor().hashCode();
    hash = (29 * hash) + unknownFields.hashCode();
    memoizedHashCode = hash;
    return hash;
  }

  public static com.buf.javamultiplefiles.unspecified.v1.UnspecifiedOuterMessageNested parseFrom(
      java.nio.ByteBuffer data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static com.buf.javamultiplefiles.unspecified.v1.UnspecifiedOuterMessageNested parseFrom(
      java.nio.ByteBuffer data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static com.buf.javamultiplefiles.unspecified.v1.UnspecifiedOuterMessageNested parseFrom(
      com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static com.buf.javamultiplefiles.unspecified.v1.UnspecifiedOuterMessageNested parseFrom(
      com.google.protobuf.ByteString data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static com.buf.javamultiplefiles.unspecified.v1.UnspecifiedOuterMessageNested parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static com.buf.javamultiplefiles.unspecified.v1.UnspecifiedOuterMessageNested parseFrom(
      byte[] data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static com.buf.javamultiplefiles.unspecified.v1.UnspecifiedOuterMessageNested parseFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
  }
  public static com.buf.javamultiplefiles.unspecified.v1.UnspecifiedOuterMessageNested parseFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input, extensionRegistry);
  }
  public static com.buf.javamultiplefiles.unspecified.v1.UnspecifiedOuterMessageNested parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input);
  }
  public static com.buf.javamultiplefiles.unspecified.v1.UnspecifiedOuterMessageNested parseDelimitedFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input, extensionRegistry);
  }
  public static com.buf.javamultiplefiles.unspecified.v1.UnspecifiedOuterMessageNested parseFrom(
      com.google.protobuf.CodedInputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
  }
  public static com.buf.javamultiplefiles.unspecified.v1.UnspecifiedOuterMessageNested parseFrom(
      com.google.protobuf.CodedInputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input, extensionRegistry);
  }

  @java.lang.Override
  public Builder newBuilderForType() { return newBuilder(); }
  public static Builder newBuilder() {
    return DEFAULT_INSTANCE.toBuilder();
  }
  public static Builder newBuilder(com.buf.javamultiplefiles.unspecified.v1.UnspecifiedOuterMessageNested prototype) {
    return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
  }
  @java.lang.Override
  public Builder toBuilder() {
    return this == DEFAULT_INSTANCE
        ? new Builder() : new Builder().mergeFrom(this);
  }

  @java.lang.Override
  protected Builder newBuilderForType(
      com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
    Builder builder = new Builder(parent);
    return builder;
  }
  /**
   * Protobuf type {@code buf.javamultiplefiles.unspecified.v1.UnspecifiedOuterMessageNested}
   */
  public static final class Builder extends
      com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
      // @@protoc_insertion_point(builder_implements:buf.javamultiplefiles.unspecified.v1.UnspecifiedOuterMessageNested)
      com.buf.javamultiplefiles.unspecified.v1.UnspecifiedOuterMessageNestedOrBuilder {
    public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
      return com.buf.javamultiplefiles.unspecified.v1.EnabledNestedProto.internal_static_buf_javamultiplefiles_unspecified_v1_UnspecifiedOuterMessageNested_descriptor;
    }

    @java.lang.Override
    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return com.buf.javamultiplefiles.unspecified.v1.EnabledNestedProto.internal_static_buf_javamultiplefiles_unspecified_v1_UnspecifiedOuterMessageNested_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              com.buf.javamultiplefiles.unspecified.v1.UnspecifiedOuterMessageNested.class, com.buf.javamultiplefiles.unspecified.v1.UnspecifiedOuterMessageNested.Builder.class);
    }

    // Construct using com.buf.javamultiplefiles.unspecified.v1.UnspecifiedOuterMessageNested.newBuilder()
    private Builder() {
      maybeForceBuilderInitialization();
    }

    private Builder(
        com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
      super(parent);
      maybeForceBuilderInitialization();
    }
    private void maybeForceBuilderInitialization() {
      if (com.google.protobuf.GeneratedMessageV3
              .alwaysUseFieldBuilders) {
      }
    }
    @java.lang.Override
    public Builder clear() {
      super.clear();
      return this;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.Descriptor
        getDescriptorForType() {
      return com.buf.javamultiplefiles.unspecified.v1.EnabledNestedProto.internal_static_buf_javamultiplefiles_unspecified_v1_UnspecifiedOuterMessageNested_descriptor;
    }

    @java.lang.Override
    public com.buf.javamultiplefiles.unspecified.v1.UnspecifiedOuterMessageNested getDefaultInstanceForType() {
      return com.buf.javamultiplefiles.unspecified.v1.UnspecifiedOuterMessageNested.getDefaultInstance();
    }

    @java.lang.Override
    public com.buf.javamultiplefiles.unspecified.v1.UnspecifiedOuterMessageNested build() {
      com.buf.javamultiplefiles.unspecified.v1.UnspecifiedOuterMessageNested result = buildPartial();
      if (!result.isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return result;
    }

    @java.lang.Override
    public com.buf.javamultiplefiles.unspecified.v1.UnspecifiedOuterMessageNested buildPartial() {
      com.buf.javamultiplefiles.unspecified.v1.UnspecifiedOuterMessageNested result = new com.buf.javamultiplefiles.unspecified.v1.UnspecifiedOuterMessageNested(this);
      onBuilt();
      return result;
    }

    @java.lang.Override
    public Builder clone() {
      return super.clone();
    }
    @java.lang.Override
    public Builder setField(
        com.google.protobuf.Descriptors.FieldDescriptor field,
        java.lang.Object value) {
      return super.setField(field, value);
    }
    @java.lang.Override
    public Builder clearField(
        com.google.protobuf.Descriptors.FieldDescriptor field) {
      return super.clearField(field);
    }
    @java.lang.Override
    public Builder clearOneof(
        com.google.protobuf.Descriptors.OneofDescriptor oneof) {
      return super.clearOneof(oneof);
    }
    @java.lang.Override
    public Builder setRepeatedField(
        com.google.protobuf.Descriptors.FieldDescriptor field,
        int index, java.lang.Object value) {
      return super.setRepeatedField(field, index, value);
    }
    @java.lang.Override
    public Builder addRepeatedField(
        com.google.protobuf.Descriptors.FieldDescriptor field,
        java.lang.Object value) {
      return super.addRepeatedField(field, value);
    }
    @java.lang.Override
    public Builder mergeFrom(com.google.protobuf.Message other) {
      if (other instanceof com.buf.javamultiplefiles.unspecified.v1.UnspecifiedOuterMessageNested) {
        return mergeFrom((com.buf.javamultiplefiles.unspecified.v1.UnspecifiedOuterMessageNested)other);
      } else {
        super.mergeFrom(other);
        return this;
      }
    }

    public Builder mergeFrom(com.buf.javamultiplefiles.unspecified.v1.UnspecifiedOuterMessageNested other) {
      if (other == com.buf.javamultiplefiles.unspecified.v1.UnspecifiedOuterMessageNested.getDefaultInstance()) return this;
      this.mergeUnknownFields(other.unknownFields);
      onChanged();
      return this;
    }

    @java.lang.Override
    public final boolean isInitialized() {
      return true;
    }

    @java.lang.Override
    public Builder mergeFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      com.buf.javamultiplefiles.unspecified.v1.UnspecifiedOuterMessageNested parsedMessage = null;
      try {
        parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        parsedMessage = (com.buf.javamultiplefiles.unspecified.v1.UnspecifiedOuterMessageNested) e.getUnfinishedMessage();
        throw e.unwrapIOException();
      } finally {
        if (parsedMessage != null) {
          mergeFrom(parsedMessage);
        }
      }
      return this;
    }
    @java.lang.Override
    public final Builder setUnknownFields(
        final com.google.protobuf.UnknownFieldSet unknownFields) {
      return super.setUnknownFields(unknownFields);
    }

    @java.lang.Override
    public final Builder mergeUnknownFields(
        final com.google.protobuf.UnknownFieldSet unknownFields) {
      return super.mergeUnknownFields(unknownFields);
    }


    // @@protoc_insertion_point(builder_scope:buf.javamultiplefiles.unspecified.v1.UnspecifiedOuterMessageNested)
  }

  // @@protoc_insertion_point(class_scope:buf.javamultiplefiles.unspecified.v1.UnspecifiedOuterMessageNested)
  private static final com.buf.javamultiplefiles.unspecified.v1.UnspecifiedOuterMessageNested DEFAULT_INSTANCE;
  static {
    DEFAULT_INSTANCE = new com.buf.javamultiplefiles.unspecified.v1.UnspecifiedOuterMessageNested();
  }

  public static com.buf.javamultiplefiles.unspecified.v1.UnspecifiedOuterMessageNested getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static final com.google.protobuf.Parser<UnspecifiedOuterMessageNested>
      PARSER = new com.google.protobuf.AbstractParser<UnspecifiedOuterMessageNested>() {
    @java.lang.Override
    public UnspecifiedOuterMessageNested parsePartialFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return new UnspecifiedOuterMessageNested(input, extensionRegistry);
    }
  };

  public static com.google.protobuf.Parser<UnspecifiedOuterMessageNested> parser() {
    return PARSER;
  }

  @java.lang.Override
  public com.google.protobuf.Parser<UnspecifiedOuterMessageNested> getParserForType() {
    return PARSER;
  }

  @java.lang.Override
  public com.buf.javamultiplefiles.unspecified.v1.UnspecifiedOuterMessageNested getDefaultInstanceForType() {
    return DEFAULT_INSTANCE;
  }

}


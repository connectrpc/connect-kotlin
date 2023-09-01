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
// source: buf/connect/demo/eliza/v1/eliza.proto

package build.buf.connect.demo.eliza.v1;

/**
 * <pre>
 * ConverseRequest describes the sentence said to the ELIZA program.
 * </pre>
 *
 * Protobuf type {@code buf.connect.demo.eliza.v1.ConverseRequest}
 */
public final class ConverseRequest extends
    com.google.protobuf.GeneratedMessageV3 implements
    // @@protoc_insertion_point(message_implements:buf.connect.demo.eliza.v1.ConverseRequest)
    ConverseRequestOrBuilder {
private static final long serialVersionUID = 0L;
  // Use ConverseRequest.newBuilder() to construct.
  private ConverseRequest(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
    super(builder);
  }
  private ConverseRequest() {
    sentence_ = "";
  }

  @java.lang.Override
  @SuppressWarnings({"unused"})
  protected java.lang.Object newInstance(
      UnusedPrivateParameter unused) {
    return new ConverseRequest();
  }

  @java.lang.Override
  public final com.google.protobuf.UnknownFieldSet
  getUnknownFields() {
    return this.unknownFields;
  }
  private ConverseRequest(
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
          case 10: {
            java.lang.String s = input.readStringRequireUtf8();

            sentence_ = s;
            break;
          }
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
    return build.buf.connect.demo.eliza.v1.ElizaProto.internal_static_buf_connect_demo_eliza_v1_ConverseRequest_descriptor;
  }

  @java.lang.Override
  protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internalGetFieldAccessorTable() {
    return build.buf.connect.demo.eliza.v1.ElizaProto.internal_static_buf_connect_demo_eliza_v1_ConverseRequest_fieldAccessorTable
        .ensureFieldAccessorsInitialized(
            build.buf.connect.demo.eliza.v1.ConverseRequest.class, build.buf.connect.demo.eliza.v1.ConverseRequest.Builder.class);
  }

  public static final int SENTENCE_FIELD_NUMBER = 1;
  private volatile java.lang.Object sentence_;
  /**
   * <code>string sentence = 1 [json_name = "sentence"];</code>
   * @return The sentence.
   */
  @java.lang.Override
  public java.lang.String getSentence() {
    java.lang.Object ref = sentence_;
    if (ref instanceof java.lang.String) {
      return (java.lang.String) ref;
    } else {
      com.google.protobuf.ByteString bs = 
          (com.google.protobuf.ByteString) ref;
      java.lang.String s = bs.toStringUtf8();
      sentence_ = s;
      return s;
    }
  }
  /**
   * <code>string sentence = 1 [json_name = "sentence"];</code>
   * @return The bytes for sentence.
   */
  @java.lang.Override
  public com.google.protobuf.ByteString
      getSentenceBytes() {
    java.lang.Object ref = sentence_;
    if (ref instanceof java.lang.String) {
      com.google.protobuf.ByteString b = 
          com.google.protobuf.ByteString.copyFromUtf8(
              (java.lang.String) ref);
      sentence_ = b;
      return b;
    } else {
      return (com.google.protobuf.ByteString) ref;
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
    if (!com.google.protobuf.GeneratedMessageV3.isStringEmpty(sentence_)) {
      com.google.protobuf.GeneratedMessageV3.writeString(output, 1, sentence_);
    }
    unknownFields.writeTo(output);
  }

  @java.lang.Override
  public int getSerializedSize() {
    int size = memoizedSize;
    if (size != -1) return size;

    size = 0;
    if (!com.google.protobuf.GeneratedMessageV3.isStringEmpty(sentence_)) {
      size += com.google.protobuf.GeneratedMessageV3.computeStringSize(1, sentence_);
    }
    size += unknownFields.getSerializedSize();
    memoizedSize = size;
    return size;
  }

  @java.lang.Override
  public boolean equals(final java.lang.Object obj) {
    if (obj == this) {
     return true;
    }
    if (!(obj instanceof build.buf.connect.demo.eliza.v1.ConverseRequest)) {
      return super.equals(obj);
    }
    build.buf.connect.demo.eliza.v1.ConverseRequest other = (build.buf.connect.demo.eliza.v1.ConverseRequest) obj;

    if (!getSentence()
        .equals(other.getSentence())) return false;
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
    hash = (37 * hash) + SENTENCE_FIELD_NUMBER;
    hash = (53 * hash) + getSentence().hashCode();
    hash = (29 * hash) + unknownFields.hashCode();
    memoizedHashCode = hash;
    return hash;
  }

  public static build.buf.connect.demo.eliza.v1.ConverseRequest parseFrom(
      java.nio.ByteBuffer data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static build.buf.connect.demo.eliza.v1.ConverseRequest parseFrom(
      java.nio.ByteBuffer data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static build.buf.connect.demo.eliza.v1.ConverseRequest parseFrom(
      com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static build.buf.connect.demo.eliza.v1.ConverseRequest parseFrom(
      com.google.protobuf.ByteString data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static build.buf.connect.demo.eliza.v1.ConverseRequest parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static build.buf.connect.demo.eliza.v1.ConverseRequest parseFrom(
      byte[] data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static build.buf.connect.demo.eliza.v1.ConverseRequest parseFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
  }
  public static build.buf.connect.demo.eliza.v1.ConverseRequest parseFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input, extensionRegistry);
  }
  public static build.buf.connect.demo.eliza.v1.ConverseRequest parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input);
  }
  public static build.buf.connect.demo.eliza.v1.ConverseRequest parseDelimitedFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input, extensionRegistry);
  }
  public static build.buf.connect.demo.eliza.v1.ConverseRequest parseFrom(
      com.google.protobuf.CodedInputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
  }
  public static build.buf.connect.demo.eliza.v1.ConverseRequest parseFrom(
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
  public static Builder newBuilder(build.buf.connect.demo.eliza.v1.ConverseRequest prototype) {
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
   * <pre>
   * ConverseRequest describes the sentence said to the ELIZA program.
   * </pre>
   *
   * Protobuf type {@code buf.connect.demo.eliza.v1.ConverseRequest}
   */
  public static final class Builder extends
      com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
      // @@protoc_insertion_point(builder_implements:buf.connect.demo.eliza.v1.ConverseRequest)
      build.buf.connect.demo.eliza.v1.ConverseRequestOrBuilder {
    public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
      return build.buf.connect.demo.eliza.v1.ElizaProto.internal_static_buf_connect_demo_eliza_v1_ConverseRequest_descriptor;
    }

    @java.lang.Override
    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return build.buf.connect.demo.eliza.v1.ElizaProto.internal_static_buf_connect_demo_eliza_v1_ConverseRequest_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              build.buf.connect.demo.eliza.v1.ConverseRequest.class, build.buf.connect.demo.eliza.v1.ConverseRequest.Builder.class);
    }

    // Construct using build.buf.connect.demo.eliza.v1.ConverseRequest.newBuilder()
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
      sentence_ = "";

      return this;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.Descriptor
        getDescriptorForType() {
      return build.buf.connect.demo.eliza.v1.ElizaProto.internal_static_buf_connect_demo_eliza_v1_ConverseRequest_descriptor;
    }

    @java.lang.Override
    public build.buf.connect.demo.eliza.v1.ConverseRequest getDefaultInstanceForType() {
      return build.buf.connect.demo.eliza.v1.ConverseRequest.getDefaultInstance();
    }

    @java.lang.Override
    public build.buf.connect.demo.eliza.v1.ConverseRequest build() {
      build.buf.connect.demo.eliza.v1.ConverseRequest result = buildPartial();
      if (!result.isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return result;
    }

    @java.lang.Override
    public build.buf.connect.demo.eliza.v1.ConverseRequest buildPartial() {
      build.buf.connect.demo.eliza.v1.ConverseRequest result = new build.buf.connect.demo.eliza.v1.ConverseRequest(this);
      result.sentence_ = sentence_;
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
      if (other instanceof build.buf.connect.demo.eliza.v1.ConverseRequest) {
        return mergeFrom((build.buf.connect.demo.eliza.v1.ConverseRequest)other);
      } else {
        super.mergeFrom(other);
        return this;
      }
    }

    public Builder mergeFrom(build.buf.connect.demo.eliza.v1.ConverseRequest other) {
      if (other == build.buf.connect.demo.eliza.v1.ConverseRequest.getDefaultInstance()) return this;
      if (!other.getSentence().isEmpty()) {
        sentence_ = other.sentence_;
        onChanged();
      }
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
      build.buf.connect.demo.eliza.v1.ConverseRequest parsedMessage = null;
      try {
        parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        parsedMessage = (build.buf.connect.demo.eliza.v1.ConverseRequest) e.getUnfinishedMessage();
        throw e.unwrapIOException();
      } finally {
        if (parsedMessage != null) {
          mergeFrom(parsedMessage);
        }
      }
      return this;
    }

    private java.lang.Object sentence_ = "";
    /**
     * <code>string sentence = 1 [json_name = "sentence"];</code>
     * @return The sentence.
     */
    public java.lang.String getSentence() {
      java.lang.Object ref = sentence_;
      if (!(ref instanceof java.lang.String)) {
        com.google.protobuf.ByteString bs =
            (com.google.protobuf.ByteString) ref;
        java.lang.String s = bs.toStringUtf8();
        sentence_ = s;
        return s;
      } else {
        return (java.lang.String) ref;
      }
    }
    /**
     * <code>string sentence = 1 [json_name = "sentence"];</code>
     * @return The bytes for sentence.
     */
    public com.google.protobuf.ByteString
        getSentenceBytes() {
      java.lang.Object ref = sentence_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b = 
            com.google.protobuf.ByteString.copyFromUtf8(
                (java.lang.String) ref);
        sentence_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }
    /**
     * <code>string sentence = 1 [json_name = "sentence"];</code>
     * @param value The sentence to set.
     * @return This builder for chaining.
     */
    public Builder setSentence(
        java.lang.String value) {
      if (value == null) {
    throw new NullPointerException();
  }
  
      sentence_ = value;
      onChanged();
      return this;
    }
    /**
     * <code>string sentence = 1 [json_name = "sentence"];</code>
     * @return This builder for chaining.
     */
    public Builder clearSentence() {
      
      sentence_ = getDefaultInstance().getSentence();
      onChanged();
      return this;
    }
    /**
     * <code>string sentence = 1 [json_name = "sentence"];</code>
     * @param value The bytes for sentence to set.
     * @return This builder for chaining.
     */
    public Builder setSentenceBytes(
        com.google.protobuf.ByteString value) {
      if (value == null) {
    throw new NullPointerException();
  }
  checkByteStringIsUtf8(value);
      
      sentence_ = value;
      onChanged();
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


    // @@protoc_insertion_point(builder_scope:buf.connect.demo.eliza.v1.ConverseRequest)
  }

  // @@protoc_insertion_point(class_scope:buf.connect.demo.eliza.v1.ConverseRequest)
  private static final build.buf.connect.demo.eliza.v1.ConverseRequest DEFAULT_INSTANCE;
  static {
    DEFAULT_INSTANCE = new build.buf.connect.demo.eliza.v1.ConverseRequest();
  }

  public static build.buf.connect.demo.eliza.v1.ConverseRequest getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static final com.google.protobuf.Parser<ConverseRequest>
      PARSER = new com.google.protobuf.AbstractParser<ConverseRequest>() {
    @java.lang.Override
    public ConverseRequest parsePartialFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return new ConverseRequest(input, extensionRegistry);
    }
  };

  public static com.google.protobuf.Parser<ConverseRequest> parser() {
    return PARSER;
  }

  @java.lang.Override
  public com.google.protobuf.Parser<ConverseRequest> getParserForType() {
    return PARSER;
  }

  @java.lang.Override
  public build.buf.connect.demo.eliza.v1.ConverseRequest getDefaultInstanceForType() {
    return DEFAULT_INSTANCE;
  }

}


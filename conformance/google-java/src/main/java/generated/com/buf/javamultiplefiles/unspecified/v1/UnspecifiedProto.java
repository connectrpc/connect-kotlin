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

public final class UnspecifiedProto {
  private UnspecifiedProto() {}
  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistryLite registry) {
  }

  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistry registry) {
    registerAllExtensions(
        (com.google.protobuf.ExtensionRegistryLite) registry);
  }
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_buf_javamultiplefiles_unspecified_v1_UnspecifiedRequest_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_buf_javamultiplefiles_unspecified_v1_UnspecifiedRequest_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_buf_javamultiplefiles_unspecified_v1_UnspecifiedResponse_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_buf_javamultiplefiles_unspecified_v1_UnspecifiedResponse_fieldAccessorTable;

  public static com.google.protobuf.Descriptors.FileDescriptor
      getDescriptor() {
    return descriptor;
  }
  private static  com.google.protobuf.Descriptors.FileDescriptor
      descriptor;
  static {
    java.lang.String[] descriptorData = {
      "\n6buf/javamultiplefiles/unspecified/v1/u" +
      "nspecified.proto\022$buf.javamultiplefiles." +
      "unspecified.v1\"0\n\022UnspecifiedRequest\022\032\n\010" +
      "sentence\030\001 \001(\tR\010sentence\"1\n\023UnspecifiedR" +
      "esponse\022\032\n\010sentence\030\001 \001(\tR\010sentence2\233\001\n\022" +
      "UnspecifiedService\022\204\001\n\013Unspecified\0228.buf" +
      ".javamultiplefiles.unspecified.v1.Unspec" +
      "ifiedRequest\0329.buf.javamultiplefiles.uns" +
      "pecified.v1.UnspecifiedResponse\"\000B\357\001\n(co" +
      "m.buf.javamultiplefiles.unspecified.v1B\020" +
      "UnspecifiedProtoP\001\242\002\003BJU\252\002$Buf.Javamulti" +
      "plefiles.Unspecified.V1\312\002$Buf\\Javamultip" +
      "lefiles\\Unspecified\\V1\342\0020Buf\\Javamultipl" +
      "efiles\\Unspecified\\V1\\GPBMetadata\352\002\'Buf:" +
      ":Javamultiplefiles::Unspecified::V1b\006pro" +
      "to3"
    };
    descriptor = com.google.protobuf.Descriptors.FileDescriptor
      .internalBuildGeneratedFileFrom(descriptorData,
        new com.google.protobuf.Descriptors.FileDescriptor[] {
        });
    internal_static_buf_javamultiplefiles_unspecified_v1_UnspecifiedRequest_descriptor =
      getDescriptor().getMessageTypes().get(0);
    internal_static_buf_javamultiplefiles_unspecified_v1_UnspecifiedRequest_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_buf_javamultiplefiles_unspecified_v1_UnspecifiedRequest_descriptor,
        new java.lang.String[] { "Sentence", });
    internal_static_buf_javamultiplefiles_unspecified_v1_UnspecifiedResponse_descriptor =
      getDescriptor().getMessageTypes().get(1);
    internal_static_buf_javamultiplefiles_unspecified_v1_UnspecifiedResponse_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_buf_javamultiplefiles_unspecified_v1_UnspecifiedResponse_descriptor,
        new java.lang.String[] { "Sentence", });
  }

  // @@protoc_insertion_point(outer_class_scope)
}

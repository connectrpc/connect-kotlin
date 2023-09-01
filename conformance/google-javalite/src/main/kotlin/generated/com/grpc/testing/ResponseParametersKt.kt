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

//Generated by the protocol buffer compiler. DO NOT EDIT!
// source: grpc/testing/messages.proto

package com.grpc.testing;

@kotlin.jvm.JvmName("-initializeresponseParameters")
public inline fun responseParameters(block: com.grpc.testing.ResponseParametersKt.Dsl.() -> kotlin.Unit): com.grpc.testing.ResponseParameters =
  com.grpc.testing.ResponseParametersKt.Dsl._create(com.grpc.testing.ResponseParameters.newBuilder()).apply { block() }._build()
public object ResponseParametersKt {
  @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
  @com.google.protobuf.kotlin.ProtoDslMarker
  public class Dsl private constructor(
    private val _builder: com.grpc.testing.ResponseParameters.Builder
  ) {
    public companion object {
      @kotlin.jvm.JvmSynthetic
      @kotlin.PublishedApi
      internal fun _create(builder: com.grpc.testing.ResponseParameters.Builder): Dsl = Dsl(builder)
    }

    @kotlin.jvm.JvmSynthetic
    @kotlin.PublishedApi
    internal fun _build(): com.grpc.testing.ResponseParameters = _builder.build()

    /**
     * <pre>
     * Desired payload sizes in responses from the server.
     * </pre>
     *
     * <code>int32 size = 1 [json_name = "size"];</code>
     */
    public var size: kotlin.Int
      @JvmName("getSize")
      get() = _builder.getSize()
      @JvmName("setSize")
      set(value) {
        _builder.setSize(value)
      }
    /**
     * <pre>
     * Desired payload sizes in responses from the server.
     * </pre>
     *
     * <code>int32 size = 1 [json_name = "size"];</code>
     */
    public fun clearSize() {
      _builder.clearSize()
    }

    /**
     * <pre>
     * Desired interval between consecutive responses in the response stream in
     * microseconds.
     * </pre>
     *
     * <code>int32 interval_us = 2 [json_name = "intervalUs"];</code>
     */
    public var intervalUs: kotlin.Int
      @JvmName("getIntervalUs")
      get() = _builder.getIntervalUs()
      @JvmName("setIntervalUs")
      set(value) {
        _builder.setIntervalUs(value)
      }
    /**
     * <pre>
     * Desired interval between consecutive responses in the response stream in
     * microseconds.
     * </pre>
     *
     * <code>int32 interval_us = 2 [json_name = "intervalUs"];</code>
     */
    public fun clearIntervalUs() {
      _builder.clearIntervalUs()
    }

    /**
     * <pre>
     * Whether to request the server to compress the response. This field is
     * "nullable" in order to interoperate seamlessly with clients not able to
     * implement the full compression tests by introspecting the call to verify
     * the response's compression status.
     * </pre>
     *
     * <code>.grpc.testing.BoolValue compressed = 3 [json_name = "compressed"];</code>
     */
    public var compressed: com.grpc.testing.BoolValue
      @JvmName("getCompressed")
      get() = _builder.getCompressed()
      @JvmName("setCompressed")
      set(value) {
        _builder.setCompressed(value)
      }
    /**
     * <pre>
     * Whether to request the server to compress the response. This field is
     * "nullable" in order to interoperate seamlessly with clients not able to
     * implement the full compression tests by introspecting the call to verify
     * the response's compression status.
     * </pre>
     *
     * <code>.grpc.testing.BoolValue compressed = 3 [json_name = "compressed"];</code>
     */
    public fun clearCompressed() {
      _builder.clearCompressed()
    }
    /**
     * <pre>
     * Whether to request the server to compress the response. This field is
     * "nullable" in order to interoperate seamlessly with clients not able to
     * implement the full compression tests by introspecting the call to verify
     * the response's compression status.
     * </pre>
     *
     * <code>.grpc.testing.BoolValue compressed = 3 [json_name = "compressed"];</code>
     * @return Whether the compressed field is set.
     */
    public fun hasCompressed(): kotlin.Boolean {
      return _builder.hasCompressed()
    }
  }
}
public inline fun com.grpc.testing.ResponseParameters.copy(block: com.grpc.testing.ResponseParametersKt.Dsl.() -> kotlin.Unit): com.grpc.testing.ResponseParameters =
  com.grpc.testing.ResponseParametersKt.Dsl._create(this.toBuilder()).apply { block() }._build()

val com.grpc.testing.ResponseParametersOrBuilder.compressedOrNull: com.grpc.testing.BoolValue?
  get() = if (hasCompressed()) getCompressed() else null


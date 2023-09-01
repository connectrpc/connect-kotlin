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

@kotlin.jvm.JvmName("-initializestreamingInputCallResponse")
public inline fun streamingInputCallResponse(block: com.grpc.testing.StreamingInputCallResponseKt.Dsl.() -> kotlin.Unit): com.grpc.testing.StreamingInputCallResponse =
  com.grpc.testing.StreamingInputCallResponseKt.Dsl._create(com.grpc.testing.StreamingInputCallResponse.newBuilder()).apply { block() }._build()
public object StreamingInputCallResponseKt {
  @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
  @com.google.protobuf.kotlin.ProtoDslMarker
  public class Dsl private constructor(
    private val _builder: com.grpc.testing.StreamingInputCallResponse.Builder
  ) {
    public companion object {
      @kotlin.jvm.JvmSynthetic
      @kotlin.PublishedApi
      internal fun _create(builder: com.grpc.testing.StreamingInputCallResponse.Builder): Dsl = Dsl(builder)
    }

    @kotlin.jvm.JvmSynthetic
    @kotlin.PublishedApi
    internal fun _build(): com.grpc.testing.StreamingInputCallResponse = _builder.build()

    /**
     * <pre>
     * Aggregated size of payloads received from the client.
     * </pre>
     *
     * <code>int32 aggregated_payload_size = 1 [json_name = "aggregatedPayloadSize"];</code>
     */
    public var aggregatedPayloadSize: kotlin.Int
      @JvmName("getAggregatedPayloadSize")
      get() = _builder.getAggregatedPayloadSize()
      @JvmName("setAggregatedPayloadSize")
      set(value) {
        _builder.setAggregatedPayloadSize(value)
      }
    /**
     * <pre>
     * Aggregated size of payloads received from the client.
     * </pre>
     *
     * <code>int32 aggregated_payload_size = 1 [json_name = "aggregatedPayloadSize"];</code>
     */
    public fun clearAggregatedPayloadSize() {
      _builder.clearAggregatedPayloadSize()
    }
  }
}
public inline fun com.grpc.testing.StreamingInputCallResponse.copy(block: com.grpc.testing.StreamingInputCallResponseKt.Dsl.() -> kotlin.Unit): com.grpc.testing.StreamingInputCallResponse =
  com.grpc.testing.StreamingInputCallResponseKt.Dsl._create(this.toBuilder()).apply { block() }._build()


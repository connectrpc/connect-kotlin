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
// source: buf/javamultiplefiles/enabled/v1/enabled_empty.proto

package com.buf.javamultiplefiles.enabled.v1;

@kotlin.jvm.JvmName("-initializeenabledEmptyRPCResponse")
public inline fun enabledEmptyRPCResponse(block: com.buf.javamultiplefiles.enabled.v1.EnabledEmptyRPCResponseKt.Dsl.() -> kotlin.Unit): com.buf.javamultiplefiles.enabled.v1.EnabledEmptyRPCResponse =
  com.buf.javamultiplefiles.enabled.v1.EnabledEmptyRPCResponseKt.Dsl._create(com.buf.javamultiplefiles.enabled.v1.EnabledEmptyRPCResponse.newBuilder()).apply { block() }._build()
public object EnabledEmptyRPCResponseKt {
  @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
  @com.google.protobuf.kotlin.ProtoDslMarker
  public class Dsl private constructor(
    private val _builder: com.buf.javamultiplefiles.enabled.v1.EnabledEmptyRPCResponse.Builder
  ) {
    public companion object {
      @kotlin.jvm.JvmSynthetic
      @kotlin.PublishedApi
      internal fun _create(builder: com.buf.javamultiplefiles.enabled.v1.EnabledEmptyRPCResponse.Builder): Dsl = Dsl(builder)
    }

    @kotlin.jvm.JvmSynthetic
    @kotlin.PublishedApi
    internal fun _build(): com.buf.javamultiplefiles.enabled.v1.EnabledEmptyRPCResponse = _builder.build()
  }
}
@kotlin.jvm.JvmSynthetic
public inline fun com.buf.javamultiplefiles.enabled.v1.EnabledEmptyRPCResponse.copy(block: com.buf.javamultiplefiles.enabled.v1.EnabledEmptyRPCResponseKt.Dsl.() -> kotlin.Unit): com.buf.javamultiplefiles.enabled.v1.EnabledEmptyRPCResponse =
  com.buf.javamultiplefiles.enabled.v1.EnabledEmptyRPCResponseKt.Dsl._create(this.toBuilder()).apply { block() }._build()


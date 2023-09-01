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
// source: buf/javamultiplefiles/unspecified/v1/unspecified.proto

package com.buf.javamultiplefiles.unspecified.v1;

@kotlin.jvm.JvmName("-initializeunspecifiedRequest")
public inline fun unspecifiedRequest(block: com.buf.javamultiplefiles.unspecified.v1.UnspecifiedRequestKt.Dsl.() -> kotlin.Unit): com.buf.javamultiplefiles.unspecified.v1.UnspecifiedRequest =
  com.buf.javamultiplefiles.unspecified.v1.UnspecifiedRequestKt.Dsl._create(com.buf.javamultiplefiles.unspecified.v1.UnspecifiedRequest.newBuilder()).apply { block() }._build()
public object UnspecifiedRequestKt {
  @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
  @com.google.protobuf.kotlin.ProtoDslMarker
  public class Dsl private constructor(
    private val _builder: com.buf.javamultiplefiles.unspecified.v1.UnspecifiedRequest.Builder
  ) {
    public companion object {
      @kotlin.jvm.JvmSynthetic
      @kotlin.PublishedApi
      internal fun _create(builder: com.buf.javamultiplefiles.unspecified.v1.UnspecifiedRequest.Builder): Dsl = Dsl(builder)
    }

    @kotlin.jvm.JvmSynthetic
    @kotlin.PublishedApi
    internal fun _build(): com.buf.javamultiplefiles.unspecified.v1.UnspecifiedRequest = _builder.build()

    /**
     * <code>string sentence = 1 [json_name = "sentence"];</code>
     */
    public var sentence: kotlin.String
      @JvmName("getSentence")
      get() = _builder.getSentence()
      @JvmName("setSentence")
      set(value) {
        _builder.setSentence(value)
      }
    /**
     * <code>string sentence = 1 [json_name = "sentence"];</code>
     */
    public fun clearSentence() {
      _builder.clearSentence()
    }
  }
}
@kotlin.jvm.JvmSynthetic
public inline fun com.buf.javamultiplefiles.unspecified.v1.UnspecifiedRequest.copy(block: com.buf.javamultiplefiles.unspecified.v1.UnspecifiedRequestKt.Dsl.() -> kotlin.Unit): com.buf.javamultiplefiles.unspecified.v1.UnspecifiedRequest =
  com.buf.javamultiplefiles.unspecified.v1.UnspecifiedRequestKt.Dsl._create(this.toBuilder()).apply { block() }._build()


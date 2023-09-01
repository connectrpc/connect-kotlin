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
// source: buf/evilcomments/v1/evilcomments.proto

package com.buf.evilcomments.v1;

@kotlin.jvm.JvmName("-initializeevilCommentsResponse")
public inline fun evilCommentsResponse(block: com.buf.evilcomments.v1.EvilCommentsResponseKt.Dsl.() -> kotlin.Unit): com.buf.evilcomments.v1.EvilCommentsResponse =
  com.buf.evilcomments.v1.EvilCommentsResponseKt.Dsl._create(com.buf.evilcomments.v1.EvilCommentsResponse.newBuilder()).apply { block() }._build()
public object EvilCommentsResponseKt {
  @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
  @com.google.protobuf.kotlin.ProtoDslMarker
  public class Dsl private constructor(
    private val _builder: com.buf.evilcomments.v1.EvilCommentsResponse.Builder
  ) {
    public companion object {
      @kotlin.jvm.JvmSynthetic
      @kotlin.PublishedApi
      internal fun _create(builder: com.buf.evilcomments.v1.EvilCommentsResponse.Builder): Dsl = Dsl(builder)
    }

    @kotlin.jvm.JvmSynthetic
    @kotlin.PublishedApi
    internal fun _build(): com.buf.evilcomments.v1.EvilCommentsResponse = _builder.build()
  }
}
public inline fun com.buf.evilcomments.v1.EvilCommentsResponse.copy(block: com.buf.evilcomments.v1.EvilCommentsResponseKt.Dsl.() -> kotlin.Unit): com.buf.evilcomments.v1.EvilCommentsResponse =
  com.buf.evilcomments.v1.EvilCommentsResponseKt.Dsl._create(this.toBuilder()).apply { block() }._build()


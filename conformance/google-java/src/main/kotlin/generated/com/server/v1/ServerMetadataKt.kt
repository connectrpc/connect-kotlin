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
// source: server/v1/server.proto

package com.server.v1;

@kotlin.jvm.JvmName("-initializeserverMetadata")
public inline fun serverMetadata(block: com.server.v1.ServerMetadataKt.Dsl.() -> kotlin.Unit): com.server.v1.ServerMetadata =
  com.server.v1.ServerMetadataKt.Dsl._create(com.server.v1.ServerMetadata.newBuilder()).apply { block() }._build()
public object ServerMetadataKt {
  @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
  @com.google.protobuf.kotlin.ProtoDslMarker
  public class Dsl private constructor(
    private val _builder: com.server.v1.ServerMetadata.Builder
  ) {
    public companion object {
      @kotlin.jvm.JvmSynthetic
      @kotlin.PublishedApi
      internal fun _create(builder: com.server.v1.ServerMetadata.Builder): Dsl = Dsl(builder)
    }

    @kotlin.jvm.JvmSynthetic
    @kotlin.PublishedApi
    internal fun _build(): com.server.v1.ServerMetadata = _builder.build()

    /**
     * <code>string host = 1 [json_name = "host"];</code>
     */
    public var host: kotlin.String
      @JvmName("getHost")
      get() = _builder.getHost()
      @JvmName("setHost")
      set(value) {
        _builder.setHost(value)
      }
    /**
     * <code>string host = 1 [json_name = "host"];</code>
     */
    public fun clearHost() {
      _builder.clearHost()
    }

    /**
     * An uninstantiable, behaviorless type to represent the field in
     * generics.
     */
    @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
    public class ProtocolsProxy private constructor() : com.google.protobuf.kotlin.DslProxy()
    /**
     * <code>repeated .server.v1.ProtocolSupport protocols = 2 [json_name = "protocols"];</code>
     */
     public val protocols: com.google.protobuf.kotlin.DslList<com.server.v1.ProtocolSupport, ProtocolsProxy>
      @kotlin.jvm.JvmSynthetic
      get() = com.google.protobuf.kotlin.DslList(
        _builder.getProtocolsList()
      )
    /**
     * <code>repeated .server.v1.ProtocolSupport protocols = 2 [json_name = "protocols"];</code>
     * @param value The protocols to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("addProtocols")
    public fun com.google.protobuf.kotlin.DslList<com.server.v1.ProtocolSupport, ProtocolsProxy>.add(value: com.server.v1.ProtocolSupport) {
      _builder.addProtocols(value)
    }
    /**
     * <code>repeated .server.v1.ProtocolSupport protocols = 2 [json_name = "protocols"];</code>
     * @param value The protocols to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("plusAssignProtocols")
    @Suppress("NOTHING_TO_INLINE")
    public inline operator fun com.google.protobuf.kotlin.DslList<com.server.v1.ProtocolSupport, ProtocolsProxy>.plusAssign(value: com.server.v1.ProtocolSupport) {
      add(value)
    }
    /**
     * <code>repeated .server.v1.ProtocolSupport protocols = 2 [json_name = "protocols"];</code>
     * @param values The protocols to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("addAllProtocols")
    public fun com.google.protobuf.kotlin.DslList<com.server.v1.ProtocolSupport, ProtocolsProxy>.addAll(values: kotlin.collections.Iterable<com.server.v1.ProtocolSupport>) {
      _builder.addAllProtocols(values)
    }
    /**
     * <code>repeated .server.v1.ProtocolSupport protocols = 2 [json_name = "protocols"];</code>
     * @param values The protocols to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("plusAssignAllProtocols")
    @Suppress("NOTHING_TO_INLINE")
    public inline operator fun com.google.protobuf.kotlin.DslList<com.server.v1.ProtocolSupport, ProtocolsProxy>.plusAssign(values: kotlin.collections.Iterable<com.server.v1.ProtocolSupport>) {
      addAll(values)
    }
    /**
     * <code>repeated .server.v1.ProtocolSupport protocols = 2 [json_name = "protocols"];</code>
     * @param index The index to set the value at.
     * @param value The protocols to set.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("setProtocols")
    public operator fun com.google.protobuf.kotlin.DslList<com.server.v1.ProtocolSupport, ProtocolsProxy>.set(index: kotlin.Int, value: com.server.v1.ProtocolSupport) {
      _builder.setProtocols(index, value)
    }
    /**
     * <code>repeated .server.v1.ProtocolSupport protocols = 2 [json_name = "protocols"];</code>
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("clearProtocols")
    public fun com.google.protobuf.kotlin.DslList<com.server.v1.ProtocolSupport, ProtocolsProxy>.clear() {
      _builder.clearProtocols()
    }

  }
}
@kotlin.jvm.JvmSynthetic
public inline fun com.server.v1.ServerMetadata.copy(block: com.server.v1.ServerMetadataKt.Dsl.() -> kotlin.Unit): com.server.v1.ServerMetadata =
  com.server.v1.ServerMetadataKt.Dsl._create(this.toBuilder()).apply { block() }._build()


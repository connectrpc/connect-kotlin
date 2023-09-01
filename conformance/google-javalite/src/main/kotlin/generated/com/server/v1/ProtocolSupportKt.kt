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

@kotlin.jvm.JvmName("-initializeprotocolSupport")
public inline fun protocolSupport(block: com.server.v1.ProtocolSupportKt.Dsl.() -> kotlin.Unit): com.server.v1.ProtocolSupport =
  com.server.v1.ProtocolSupportKt.Dsl._create(com.server.v1.ProtocolSupport.newBuilder()).apply { block() }._build()
public object ProtocolSupportKt {
  @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
  @com.google.protobuf.kotlin.ProtoDslMarker
  public class Dsl private constructor(
    private val _builder: com.server.v1.ProtocolSupport.Builder
  ) {
    public companion object {
      @kotlin.jvm.JvmSynthetic
      @kotlin.PublishedApi
      internal fun _create(builder: com.server.v1.ProtocolSupport.Builder): Dsl = Dsl(builder)
    }

    @kotlin.jvm.JvmSynthetic
    @kotlin.PublishedApi
    internal fun _build(): com.server.v1.ProtocolSupport = _builder.build()

    /**
     * <code>.server.v1.Protocol protocol = 1 [json_name = "protocol"];</code>
     */
    public var protocol: com.server.v1.Protocol
      @JvmName("getProtocol")
      get() = _builder.getProtocol()
      @JvmName("setProtocol")
      set(value) {
        _builder.setProtocol(value)
      }
    /**
     * <code>.server.v1.Protocol protocol = 1 [json_name = "protocol"];</code>
     */
    public fun clearProtocol() {
      _builder.clearProtocol()
    }

    /**
     * An uninstantiable, behaviorless type to represent the field in
     * generics.
     */
    @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
    public class HttpVersionsProxy private constructor() : com.google.protobuf.kotlin.DslProxy()
    /**
     * <code>repeated .server.v1.HTTPVersion http_versions = 2 [json_name = "httpVersions"];</code>
     */
     public val httpVersions: com.google.protobuf.kotlin.DslList<com.server.v1.HTTPVersion, HttpVersionsProxy>
      @kotlin.jvm.JvmSynthetic
      get() = com.google.protobuf.kotlin.DslList(
        _builder.getHttpVersionsList()
      )
    /**
     * <code>repeated .server.v1.HTTPVersion http_versions = 2 [json_name = "httpVersions"];</code>
     * @param value The httpVersions to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("addHttpVersions")
    public fun com.google.protobuf.kotlin.DslList<com.server.v1.HTTPVersion, HttpVersionsProxy>.add(value: com.server.v1.HTTPVersion) {
      _builder.addHttpVersions(value)
    }
    /**
     * <code>repeated .server.v1.HTTPVersion http_versions = 2 [json_name = "httpVersions"];</code>
     * @param value The httpVersions to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("plusAssignHttpVersions")
    @Suppress("NOTHING_TO_INLINE")
    public inline operator fun com.google.protobuf.kotlin.DslList<com.server.v1.HTTPVersion, HttpVersionsProxy>.plusAssign(value: com.server.v1.HTTPVersion) {
      add(value)
    }
    /**
     * <code>repeated .server.v1.HTTPVersion http_versions = 2 [json_name = "httpVersions"];</code>
     * @param values The httpVersions to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("addAllHttpVersions")
    public fun com.google.protobuf.kotlin.DslList<com.server.v1.HTTPVersion, HttpVersionsProxy>.addAll(values: kotlin.collections.Iterable<com.server.v1.HTTPVersion>) {
      _builder.addAllHttpVersions(values)
    }
    /**
     * <code>repeated .server.v1.HTTPVersion http_versions = 2 [json_name = "httpVersions"];</code>
     * @param values The httpVersions to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("plusAssignAllHttpVersions")
    @Suppress("NOTHING_TO_INLINE")
    public inline operator fun com.google.protobuf.kotlin.DslList<com.server.v1.HTTPVersion, HttpVersionsProxy>.plusAssign(values: kotlin.collections.Iterable<com.server.v1.HTTPVersion>) {
      addAll(values)
    }
    /**
     * <code>repeated .server.v1.HTTPVersion http_versions = 2 [json_name = "httpVersions"];</code>
     * @param index The index to set the value at.
     * @param value The httpVersions to set.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("setHttpVersions")
    public operator fun com.google.protobuf.kotlin.DslList<com.server.v1.HTTPVersion, HttpVersionsProxy>.set(index: kotlin.Int, value: com.server.v1.HTTPVersion) {
      _builder.setHttpVersions(index, value)
    }
    /**
     * <code>repeated .server.v1.HTTPVersion http_versions = 2 [json_name = "httpVersions"];</code>
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("clearHttpVersions")
    public fun com.google.protobuf.kotlin.DslList<com.server.v1.HTTPVersion, HttpVersionsProxy>.clear() {
      _builder.clearHttpVersions()
    }

    /**
     * <code>string port = 3 [json_name = "port"];</code>
     */
    public var port: kotlin.String
      @JvmName("getPort")
      get() = _builder.getPort()
      @JvmName("setPort")
      set(value) {
        _builder.setPort(value)
      }
    /**
     * <code>string port = 3 [json_name = "port"];</code>
     */
    public fun clearPort() {
      _builder.clearPort()
    }
  }
}
public inline fun com.server.v1.ProtocolSupport.copy(block: com.server.v1.ProtocolSupportKt.Dsl.() -> kotlin.Unit): com.server.v1.ProtocolSupport =
  com.server.v1.ProtocolSupportKt.Dsl._create(this.toBuilder()).apply { block() }._build()


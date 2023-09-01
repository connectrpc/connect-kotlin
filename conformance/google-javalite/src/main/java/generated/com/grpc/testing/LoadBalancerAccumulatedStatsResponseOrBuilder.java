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
// source: grpc/testing/messages.proto

package com.grpc.testing;

public interface LoadBalancerAccumulatedStatsResponseOrBuilder extends
    // @@protoc_insertion_point(interface_extends:grpc.testing.LoadBalancerAccumulatedStatsResponse)
    com.google.protobuf.MessageLiteOrBuilder {

  /**
   * <pre>
   * The total number of RPCs have ever issued for each type.
   * Deprecated: use stats_per_method.rpcs_started instead.
   * </pre>
   *
   * <code>map&lt;string, int32&gt; num_rpcs_started_by_method = 1 [json_name = "numRpcsStartedByMethod", deprecated = true];</code>
   */
  @java.lang.Deprecated int getNumRpcsStartedByMethodCount();
  /**
   * <pre>
   * The total number of RPCs have ever issued for each type.
   * Deprecated: use stats_per_method.rpcs_started instead.
   * </pre>
   *
   * <code>map&lt;string, int32&gt; num_rpcs_started_by_method = 1 [json_name = "numRpcsStartedByMethod", deprecated = true];</code>
   */
  @java.lang.Deprecated boolean containsNumRpcsStartedByMethod(
      java.lang.String key);
  /**
   * Use {@link #getNumRpcsStartedByMethodMap()} instead.
   */
  @java.lang.Deprecated
  java.util.Map<java.lang.String, java.lang.Integer>
  getNumRpcsStartedByMethod();
  /**
   * <pre>
   * The total number of RPCs have ever issued for each type.
   * Deprecated: use stats_per_method.rpcs_started instead.
   * </pre>
   *
   * <code>map&lt;string, int32&gt; num_rpcs_started_by_method = 1 [json_name = "numRpcsStartedByMethod", deprecated = true];</code>
   */
  @java.lang.Deprecated java.util.Map<java.lang.String, java.lang.Integer>
  getNumRpcsStartedByMethodMap();
  /**
   * <pre>
   * The total number of RPCs have ever issued for each type.
   * Deprecated: use stats_per_method.rpcs_started instead.
   * </pre>
   *
   * <code>map&lt;string, int32&gt; num_rpcs_started_by_method = 1 [json_name = "numRpcsStartedByMethod", deprecated = true];</code>
   */
  @java.lang.Deprecated 
  int getNumRpcsStartedByMethodOrDefault(
      java.lang.String key,
      int defaultValue);
  /**
   * <pre>
   * The total number of RPCs have ever issued for each type.
   * Deprecated: use stats_per_method.rpcs_started instead.
   * </pre>
   *
   * <code>map&lt;string, int32&gt; num_rpcs_started_by_method = 1 [json_name = "numRpcsStartedByMethod", deprecated = true];</code>
   */
  @java.lang.Deprecated 
  int getNumRpcsStartedByMethodOrThrow(
      java.lang.String key);

  /**
   * <pre>
   * The total number of RPCs have ever completed successfully for each type.
   * Deprecated: use stats_per_method.result instead.
   * </pre>
   *
   * <code>map&lt;string, int32&gt; num_rpcs_succeeded_by_method = 2 [json_name = "numRpcsSucceededByMethod", deprecated = true];</code>
   */
  @java.lang.Deprecated int getNumRpcsSucceededByMethodCount();
  /**
   * <pre>
   * The total number of RPCs have ever completed successfully for each type.
   * Deprecated: use stats_per_method.result instead.
   * </pre>
   *
   * <code>map&lt;string, int32&gt; num_rpcs_succeeded_by_method = 2 [json_name = "numRpcsSucceededByMethod", deprecated = true];</code>
   */
  @java.lang.Deprecated boolean containsNumRpcsSucceededByMethod(
      java.lang.String key);
  /**
   * Use {@link #getNumRpcsSucceededByMethodMap()} instead.
   */
  @java.lang.Deprecated
  java.util.Map<java.lang.String, java.lang.Integer>
  getNumRpcsSucceededByMethod();
  /**
   * <pre>
   * The total number of RPCs have ever completed successfully for each type.
   * Deprecated: use stats_per_method.result instead.
   * </pre>
   *
   * <code>map&lt;string, int32&gt; num_rpcs_succeeded_by_method = 2 [json_name = "numRpcsSucceededByMethod", deprecated = true];</code>
   */
  @java.lang.Deprecated java.util.Map<java.lang.String, java.lang.Integer>
  getNumRpcsSucceededByMethodMap();
  /**
   * <pre>
   * The total number of RPCs have ever completed successfully for each type.
   * Deprecated: use stats_per_method.result instead.
   * </pre>
   *
   * <code>map&lt;string, int32&gt; num_rpcs_succeeded_by_method = 2 [json_name = "numRpcsSucceededByMethod", deprecated = true];</code>
   */
  @java.lang.Deprecated 
  int getNumRpcsSucceededByMethodOrDefault(
      java.lang.String key,
      int defaultValue);
  /**
   * <pre>
   * The total number of RPCs have ever completed successfully for each type.
   * Deprecated: use stats_per_method.result instead.
   * </pre>
   *
   * <code>map&lt;string, int32&gt; num_rpcs_succeeded_by_method = 2 [json_name = "numRpcsSucceededByMethod", deprecated = true];</code>
   */
  @java.lang.Deprecated 
  int getNumRpcsSucceededByMethodOrThrow(
      java.lang.String key);

  /**
   * <pre>
   * The total number of RPCs have ever failed for each type.
   * Deprecated: use stats_per_method.result instead.
   * </pre>
   *
   * <code>map&lt;string, int32&gt; num_rpcs_failed_by_method = 3 [json_name = "numRpcsFailedByMethod", deprecated = true];</code>
   */
  @java.lang.Deprecated int getNumRpcsFailedByMethodCount();
  /**
   * <pre>
   * The total number of RPCs have ever failed for each type.
   * Deprecated: use stats_per_method.result instead.
   * </pre>
   *
   * <code>map&lt;string, int32&gt; num_rpcs_failed_by_method = 3 [json_name = "numRpcsFailedByMethod", deprecated = true];</code>
   */
  @java.lang.Deprecated boolean containsNumRpcsFailedByMethod(
      java.lang.String key);
  /**
   * Use {@link #getNumRpcsFailedByMethodMap()} instead.
   */
  @java.lang.Deprecated
  java.util.Map<java.lang.String, java.lang.Integer>
  getNumRpcsFailedByMethod();
  /**
   * <pre>
   * The total number of RPCs have ever failed for each type.
   * Deprecated: use stats_per_method.result instead.
   * </pre>
   *
   * <code>map&lt;string, int32&gt; num_rpcs_failed_by_method = 3 [json_name = "numRpcsFailedByMethod", deprecated = true];</code>
   */
  @java.lang.Deprecated java.util.Map<java.lang.String, java.lang.Integer>
  getNumRpcsFailedByMethodMap();
  /**
   * <pre>
   * The total number of RPCs have ever failed for each type.
   * Deprecated: use stats_per_method.result instead.
   * </pre>
   *
   * <code>map&lt;string, int32&gt; num_rpcs_failed_by_method = 3 [json_name = "numRpcsFailedByMethod", deprecated = true];</code>
   */
  @java.lang.Deprecated 
  int getNumRpcsFailedByMethodOrDefault(
      java.lang.String key,
      int defaultValue);
  /**
   * <pre>
   * The total number of RPCs have ever failed for each type.
   * Deprecated: use stats_per_method.result instead.
   * </pre>
   *
   * <code>map&lt;string, int32&gt; num_rpcs_failed_by_method = 3 [json_name = "numRpcsFailedByMethod", deprecated = true];</code>
   */
  @java.lang.Deprecated 
  int getNumRpcsFailedByMethodOrThrow(
      java.lang.String key);

  /**
   * <pre>
   * Per-method RPC statistics.  The key is the RpcType in string form; e.g.
   * 'EMPTY_CALL' or 'UNARY_CALL'
   * </pre>
   *
   * <code>map&lt;string, .grpc.testing.LoadBalancerAccumulatedStatsResponse.MethodStats&gt; stats_per_method = 4 [json_name = "statsPerMethod"];</code>
   */
  int getStatsPerMethodCount();
  /**
   * <pre>
   * Per-method RPC statistics.  The key is the RpcType in string form; e.g.
   * 'EMPTY_CALL' or 'UNARY_CALL'
   * </pre>
   *
   * <code>map&lt;string, .grpc.testing.LoadBalancerAccumulatedStatsResponse.MethodStats&gt; stats_per_method = 4 [json_name = "statsPerMethod"];</code>
   */
  boolean containsStatsPerMethod(
      java.lang.String key);
  /**
   * Use {@link #getStatsPerMethodMap()} instead.
   */
  @java.lang.Deprecated
  java.util.Map<java.lang.String, com.grpc.testing.LoadBalancerAccumulatedStatsResponse.MethodStats>
  getStatsPerMethod();
  /**
   * <pre>
   * Per-method RPC statistics.  The key is the RpcType in string form; e.g.
   * 'EMPTY_CALL' or 'UNARY_CALL'
   * </pre>
   *
   * <code>map&lt;string, .grpc.testing.LoadBalancerAccumulatedStatsResponse.MethodStats&gt; stats_per_method = 4 [json_name = "statsPerMethod"];</code>
   */
  java.util.Map<java.lang.String, com.grpc.testing.LoadBalancerAccumulatedStatsResponse.MethodStats>
  getStatsPerMethodMap();
  /**
   * <pre>
   * Per-method RPC statistics.  The key is the RpcType in string form; e.g.
   * 'EMPTY_CALL' or 'UNARY_CALL'
   * </pre>
   *
   * <code>map&lt;string, .grpc.testing.LoadBalancerAccumulatedStatsResponse.MethodStats&gt; stats_per_method = 4 [json_name = "statsPerMethod"];</code>
   */

  /* nullable */
com.grpc.testing.LoadBalancerAccumulatedStatsResponse.MethodStats getStatsPerMethodOrDefault(
      java.lang.String key,
      /* nullable */
com.grpc.testing.LoadBalancerAccumulatedStatsResponse.MethodStats defaultValue);
  /**
   * <pre>
   * Per-method RPC statistics.  The key is the RpcType in string form; e.g.
   * 'EMPTY_CALL' or 'UNARY_CALL'
   * </pre>
   *
   * <code>map&lt;string, .grpc.testing.LoadBalancerAccumulatedStatsResponse.MethodStats&gt; stats_per_method = 4 [json_name = "statsPerMethod"];</code>
   */

  com.grpc.testing.LoadBalancerAccumulatedStatsResponse.MethodStats getStatsPerMethodOrThrow(
      java.lang.String key);
}

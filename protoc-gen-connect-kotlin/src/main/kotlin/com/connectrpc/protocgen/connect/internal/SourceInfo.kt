// Copyright 2022-2023 The Connect Authors
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

package com.connectrpc.protocgen.connect.internal

import com.google.protobuf.DescriptorProtos

internal class SourceInfo(
    private val helper: SourceCodeHelper,
    private val descriptorSource: Plugin.DescriptorSource,
    path: List<Int> = emptyList()
) {
    constructor(
        fileDescriptor: DescriptorProtos.FileDescriptorProto,
        descriptorSource: Plugin.DescriptorSource,
        path: List<Int> = emptyList()
    ) : this(SourceCodeHelper(fileDescriptor), descriptorSource, path)

    private val path = listOf(*path.toTypedArray())

    fun push(value: Int): SourceInfo {
        return SourceInfo(helper, descriptorSource, path.plus(value))
    }

    fun comment(): String {
        return helper.getComment(path)
    }
}

/**
 * Creates an index for the source locations.
 */
internal class SourceCodeHelper(
    fileDescriptorProto: DescriptorProtos.FileDescriptorProto
) {
    private val locations: Map<List<Int>, List<DescriptorProtos.SourceCodeInfo.Location>> = makeLocationMap(fileDescriptorProto.sourceCodeInfo.locationList)

    /**
     * The location of the first span associated with the given path.
     */
    fun getComment(path: List<Int>): String {
        val location = locations[path]?.firstOrNull()
        var comment = location?.leadingComments
        if (comment.isNullOrBlank()) {
            comment = location?.trailingComments
        }
        return comment ?: ""
    }

    private fun makeLocationMap(locationList: List<DescriptorProtos.SourceCodeInfo.Location>): Map<List<Int>, List<DescriptorProtos.SourceCodeInfo.Location>> {
        val locationMap = mutableMapOf<List<Int>, MutableList<DescriptorProtos.SourceCodeInfo.Location>>()
        for (location in locationList) {
            val path = mutableListOf<Int>()
            for (pathElement in location.pathList) {
                path.add(pathElement)
            }
            val locList = locationMap.getOrPut(path) { mutableListOf() }
            locList.add(location)
        }
        return locationMap
    }
}

/**
 * Primarily for associating source information for a specific elements.
 *
 * @param parentSourceInfo source information for the parent element.
 * @param childTag tag of the field in the parent that corresponds with the list of child element.
 * @return zipped list of source information associated with each element in the list.
 */
internal fun <T> List<T>.withSourceInfo(parentSourceInfo: SourceInfo, childTag: Int): List<Pair<SourceInfo, T>> {
    val baseSource = parentSourceInfo.push(childTag)
    val result = mutableListOf<Pair<SourceInfo, T>>()
    for ((index, element) in withIndex()) {
        val newSource = baseSource.push(index)
        result.add(newSource to element)
    }
    return result
}

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

import com.google.protobuf.Descriptors
import com.google.protobuf.Descriptors.FileDescriptor

/**
 * Various helper methods, particularly useful when generating Java code.
 * Original code from: https://codereview.appspot.com/912042.
 *
 * @author t.broyer@ltgt.net Thomas Broyer
 * Based on the initial work of:
 *  @author kenton@google.com Kenton Varda
 * Based on original Protocol Buffers design by
 *  Sanjay Ghemawat, Jeff Dean, and others.
 */

private const val OUTER_CLASS_SUFFIX = "OuterClass"

/**
 * Parses a set of comma-delimited name/value pairs.
 *
 *
 * Several code generators treat the parameter argument as holding a list of
 * options separated by commas: e.g., `"foo=bar,baz,qux=corge"` parses
 * to the pairs: `("foo", "bar"), ("baz", ""), ("qux", "corge")`.
 *
 *
 * When a key is present several times, only the last value is retained.
 */
internal fun parseGeneratorParameter(
    text: String,
): Map<String, String> {
    if (text.isEmpty()) {
        return emptyMap()
    }
    val result: MutableMap<String, String> = HashMap()
    val parts = text.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
    for (part in parts) {
        if (part.isEmpty()) {
            continue
        }
        val equalsPos = part.indexOf('=')
        var key: String
        var value: String
        if (equalsPos < 0) {
            key = part
            value = ""
        } else {
            key = part.substring(0, equalsPos)
            value = part.substring(equalsPos + 1)
        }
        val normalizedKey = underscoresToCamelCaseImpl(key, false)
        result[normalizedKey] = value
    }
    return result
}

/**
 * Returns the Java file name (and path) for a given message.
 *
 * This depends on the `java_package`, `package` and
 * `java_multiple_files` options specified in the .proto file.
 */
internal fun getProtocJavaFileName(descriptor: Descriptors.Descriptor): String {
    var descriptor = descriptor
    val fullName: String
    if (descriptor.file.options.javaMultipleFiles) {
        var containingType: Descriptors.Descriptor
        while (descriptor.containingType.also { containingType = it } != null) {
            descriptor = containingType
        }
        fullName = getClassName(descriptor)
    } else {
        fullName = getClassNameForFile(descriptor.file)
    }
    return fullName.replace('.', '/') + ".java"
}

/**
 * Returns the Java file name (and path) for a given enum.
 *
 * This depends on the `java_package`, `package` and
 * `java_multiple_files` options specified in the .proto file.
 */
internal fun getProtocJavaFileName(descriptor: Descriptors.EnumDescriptor): String {
    if (descriptor.containingType != null) {
        return getProtocJavaFileName(descriptor.containingType)
    }
    val fullName: String = if (descriptor.file.options.javaMultipleFiles) {
        getClassName(descriptor)
    } else {
        getClassNameForFile(descriptor.file)
    }
    return fullName.replace('.', '/') + ".java"
}

/**
 * Returns the Java file name (and path) for a given service.
 *
 * This depends on the `java_package`, `package` and
 * `java_multiple_files` options specified in the .proto file.
 */
internal fun getProtocJavaFileName(descriptor: Descriptors.ServiceDescriptor): String {
    val fullName: String = if (descriptor.file.options.javaMultipleFiles) {
        getClassName(descriptor)
    } else {
        getClassNameForFile(descriptor.file)
    }
    return fullName.replace('.', '/') + ".java"
}

/**
 * Strips ".proto" or ".protodevel" from the end of a filename.
 */
private fun stripProto(filename: String): String {
    return if (filename.endsWith(".protodevel")) {
        filename.substring(0, filename.length - ".protodevel".length)
    } else if (filename.endsWith(".proto")) {
        filename.substring(0, filename.length - ".proto".length)
    } else {
        filename
    }
}

/**
 * Gets the unqualified class name for the file.
 *
 * Each .proto file becomes a single Java class, with all its contents nested
 * in that class, unless the `java_multiple_files` option has been set
 * to true.
 */
internal fun getFileClassName(file: FileDescriptor): String {
    return if (file.options.hasJavaOuterClassname()) {
        file.options.javaOuterClassname
    } else {
        var basename = file.name
        val lastSlash = basename.lastIndexOf('/')
        if (lastSlash >= 0) {
            basename = basename.substring(lastSlash + 1)
        }
        val className = underscoresToCamelCaseImpl(stripProto(basename), true)
        return if (hasConflictingClassName(file, className)) {
            "$className$OUTER_CLASS_SUFFIX"
        } else {
            className
        }
    }
}

/**
 * Checks if the file has a conflicting class name.
 *
 * This is primarily used to identify when the Google generators are
 * generating for `java_multiple_files=false`. If there exists a message
 * with the same name as the protobuf file (e.g. `message Empty {}` in `empty.proto`)
 * the generated file becomes suffixed with "OuterClass". This helper function
 * identifies when a conflict could occur so that the caller can make a decision on
 * what to do.
 *
 * Translated from:
 * https://github.com/protocolbuffers/protobuf/blob/main/src/google/protobuf/compiler/java/name_resolver.cc#L195-L217.
 */
private fun hasConflictingClassName(file: FileDescriptor, className: String): Boolean {
    for (enum in file.enumTypes) {
        if (enum.name.equals(className)) {
            return true
        }
    }
    for (service in file.services) {
        if (service.name.equals(className)) {
            return true
        }
    }
    for (messages in file.messageTypes) {
        if (messages.name.equals(className)) {
            return true
        }
    }
    return false
}

/**
 * Returns the file's Java package name.
 *
 * This depends on the `java_package` and `package` and options
 * specified in the .proto file.
 */
internal fun getFileJavaPackage(file: FileDescriptor): String {
    return if (file.options.hasJavaPackage()) {
        file.options.javaPackage
    } else {
        file.getPackage()
    }
}

/**
 * Returns the Java file name (and path) for a given message.
 *
 * This depends on the `java_package`, `package` and
 * `java_multiple_files` options specified in the .proto file.
 */
fun getJavaFileName(descriptor: Descriptors.Descriptor): String {
    var descriptor: Descriptors.Descriptor = descriptor
    val fullName: String
    if (descriptor.file.options.javaMultipleFiles) {
        var containingType: Descriptors.Descriptor?
        while (descriptor.containingType.also { containingType = it } != null) {
            descriptor = containingType!!
        }
        fullName = getClassName(descriptor)
    } else {
        fullName = getClassNameForFile(descriptor.file)
    }
    return fullName.replace('.', '/') + ".java"
}

/**
 * Converts the given fully-qualified name in the proto namespace to its
 * fully-qualified name in the Java namespace, given that it is in the given
 * file.
 */
private fun toJavaName(fullName: String, file: FileDescriptor): String {
    val result = StringBuilder()
    if (file.options.javaMultipleFiles) {
        result.append(getFileJavaPackage(file))
    } else {
        result.append(getClassNameForFile(file))
    }
    if (result.isNotEmpty()) {
        result.append('.')
    }
    if (file.getPackage().isEmpty()) {
        result.append(fullName)
    } else {
        // Strip the proto package from full_name since we've replaced it
        // with the Java package.
        result.append(fullName.substring(file.getPackage().length + 1))
    }
    return result.toString()
}

/**
 * Returns the fully-qualified class name corresponding to the given
 * message descriptor.
 */
internal fun getClassName(descriptor: Descriptors.Descriptor): String {
    return toJavaName(descriptor.fullName, descriptor.file)
}

/**
 * Returns the fully-qualified class name corresponding to the given
 * enum descriptor.
 */
internal fun getClassName(descriptor: Descriptors.EnumDescriptor): String {
    return toJavaName(descriptor.fullName, descriptor.file)
}

/**
 * Returns the fully-qualified class name corresponding to the given
 * service descriptor.
 */
internal fun getClassName(descriptor: Descriptors.ServiceDescriptor): String {
    return toJavaName(descriptor.fullName, descriptor.file)
}

/**
 * Returns the fully-qualified class name corresponding to the given
 * file descriptor.
 */
internal fun getClassNameForFile(descriptor: FileDescriptor): String {
    val result = StringBuilder(getFileJavaPackage(descriptor))
    if (result.isNotEmpty()) {
        result.append('.')
    }
    result.append(getFileClassName(descriptor))
    return result.toString()
}

/**
 * Returns the unqualified name that should be used for a field's field
 * number constant.
 */
internal fun getFieldConstantName(field: Descriptors.FieldDescriptor): String {
    return field.name.uppercase() + "_FIELD_NUMBER"
}

/**
 * Returns the name for the given field (special-casing groups).
 */
private fun getFieldName(field: Descriptors.FieldDescriptor): String {
    // Groups are hacky: The name of the field is just the lower-cased name
    // of the group type. In Java, though, we would like to retain the
    // original capitalization of the type name.
    return if (field.type == Descriptors.FieldDescriptor.Type.GROUP) {
        field.messageType.name
    } else {
        field.name
    }
}

/**
 * Converts the given input to camel-case, specifying whether the first
 * letter should be upper-case or lower-case.
 *
 * @param input         string to be converted
 * @param capNextLetter `true` if the first letter should be turned to
 * upper-case.
 * @return the camel-cased string
 */
private fun underscoresToCamelCaseImpl(
    input: String,
    capNextLetter: Boolean,
): String {
    var capNextLetter = capNextLetter
    val result = StringBuilder(input.length)
    var i = 0
    val l = input.length
    while (i < l) {
        val c = input[i]
        capNextLetter = if ('a' <= c && c <= 'z') {
            if (capNextLetter) {
                result.append((c.code + ('A'.code - 'a'.code)).toChar())
            } else {
                result.append(c)
            }
            false
        } else if ('A' <= c && c <= 'Z') {
            if (i == 0 && !capNextLetter) {
                // Force first letter to lower-case unless explicitly told
                // to capitalize it.
                result.append((c.code + ('a'.code - 'A'.code)).toChar())
            } else {
                // Capital letters after the first are left as-is.
                result.append(c)
            }
            false
        } else if ('0' <= c && c <= '9') {
            result.append(c)
            true
        } else {
            true
        }
        i++
    }
    return result.toString()
}

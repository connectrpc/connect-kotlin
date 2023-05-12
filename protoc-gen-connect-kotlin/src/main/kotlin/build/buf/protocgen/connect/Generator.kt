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

package build.buf.protocgen.connect

import build.buf.connect.BidirectionalStreamInterface
import build.buf.connect.ClientOnlyStreamInterface
import build.buf.connect.Idempotency
import build.buf.connect.MethodSpec
import build.buf.connect.ProtocolClientInterface
import build.buf.connect.ResponseMessage
import build.buf.connect.ServerOnlyStreamInterface
import build.buf.protocgen.connect.internal.CodeGenerator
import build.buf.protocgen.connect.internal.Configuration
import build.buf.protocgen.connect.internal.Plugin
import build.buf.protocgen.connect.internal.SourceInfo
import build.buf.protocgen.connect.internal.getClassName
import build.buf.protocgen.connect.internal.getFileJavaPackage
import build.buf.protocgen.connect.internal.parse
import build.buf.protocgen.connect.internal.withSourceInfo
import com.google.protobuf.DescriptorProtos
import com.google.protobuf.DescriptorProtos.FileDescriptorProto
import com.google.protobuf.Descriptors
import com.google.protobuf.compiler.PluginProtos
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName

/*
 * These are constants since build.buf.connect.Headers and build.buf.connect.http.Cancelable
 * are type aliases which doesn't have an underlying class for KotlinPoet to know what to do.
 *
 * The conventional and nicer way is to use the class type: Headers::class.asClassType() but
 * type aliasing does not allow for that.
 *
 * Instead, this is the way to reference these objects for now. If there is ever a desire to
 * move off of type aliases, this can be changed without user API breakage.
 */
private val HEADERS_CLASS_NAME = ClassName("build.buf.connect", "Headers")
private val CANCELABLE_CLASS_NAME = ClassName("build.buf.connect.http", "Cancelable")

class Generator : CodeGenerator {
    private lateinit var descriptorSource: Plugin.DescriptorSource
    private lateinit var configuration: Configuration
    private val protoFileMap = mutableMapOf<String, FileDescriptorProto>()

    override fun generate(
        request: PluginProtos.CodeGeneratorRequest,
        descriptorSource: Plugin.DescriptorSource,
        response: Plugin.Response
    ) {
        this.descriptorSource = descriptorSource
        configuration = parse(request.parameter)
        for (protoFile in request.protoFileList) {
            protoFileMap.put(protoFile.name, protoFile)
        }
        for (fileName in request.fileToGenerateList) {
            val file =
                descriptorSource.findFileByName(fileName) ?: throw RuntimeException("no descriptor sources found.")
            if (file.services.isEmpty()) {
                // Avoid generating files with no service definitions.
                continue
            }
            val fileMap = parseFile(file)
            for ((className, fileSpec) in fileMap) {
                try {
                    response.addFile("${className.canonicalName.packageToDirectory()}.kt", fileSpec.toString())
                } catch (e: Throwable) {
                    throw Throwable("failure on generating ${file.name}", e)
                }
            }
        }
    }

    private fun parseFile(file: Descriptors.FileDescriptor): Map<ClassName, FileSpec> {
        val baseSourceInfo = SourceInfo(protoFileMap[file.name]!!, descriptorSource, emptyList())
        val fileSpecs = mutableMapOf<ClassName, FileSpec>()
        val packageName = getFileJavaPackage(file)
        for ((sourceInfo, service) in file.services.withSourceInfo(
            baseSourceInfo,
            FileDescriptorProto.SERVICE_FIELD_NUMBER
        )) {
            val interfaceFileSpec = FileSpec.builder(packageName, file.name)
                // Manually import `method()` since it is a method and not a class.
                .addFileComment("Code generated by connect-kotlin. DO NOT EDIT.\n")
                .addFileComment("\n")
                .addFileComment("Source: ${file.name}\n")
                .addType(serviceClientInterface(packageName, service, sourceInfo))
                .build()
            fileSpecs.put(serviceClientInterfaceClassName(packageName, service), interfaceFileSpec)

            val implementationFileSpecBuilder = FileSpec.builder(packageName, file.name)
                // Manually import `method()` since it is a method and not a class.
                .addImport(MethodSpec::class.java.packageName, "MethodSpec")
                .addFileComment("Code generated by connect-kotlin. DO NOT EDIT.\n")
                .addFileComment("\n")
                .addFileComment("Source: ${file.name}\n")
                // Set the file package for the generated methods.
                .addType(serviceClientImplementation(packageName, service, sourceInfo))
            for (method in service.methods) {
                if (method.options.hasIdempotencyLevel()) {
                    implementationFileSpecBuilder.addImport(Idempotency::class.java, "IDEMPOTENCY_UNKNOWN", "NO_SIDE_EFFECTS", "IDEMPOTENT")
                    break
                }
            }
            val implementationFileSpec = implementationFileSpecBuilder.build()
            fileSpecs.put(serviceClientImplementationClassName(packageName, service), implementationFileSpec)
        }
        return fileSpecs
    }

    private fun serviceClientInterface(
        packageName: String,
        service: Descriptors.ServiceDescriptor,
        sourceInfo: SourceInfo
    ): TypeSpec {
        val interfaceBuilder = TypeSpec.interfaceBuilder(serviceClientInterfaceClassName(packageName, service))
        val functionSpecs = interfaceMethods(service.methods, sourceInfo)
        return interfaceBuilder
            .addKdoc(sourceInfo.comment())
            .addFunctions(functionSpecs)
            .build()
    }

    private fun interfaceMethods(
        methods: List<Descriptors.MethodDescriptor>,
        baseSourceInfo: SourceInfo
    ): List<FunSpec> {
        val functions = mutableListOf<FunSpec>()
        val headerParameterSpec = ParameterSpec.builder("headers", HEADERS_CLASS_NAME)
            .defaultValue("%L", "emptyMap()")
            .build()
        for ((sourceInfo, method) in methods.withSourceInfo(
            baseSourceInfo,
            DescriptorProtos.ServiceDescriptorProto.METHOD_FIELD_NUMBER
        )) {
            val inputClassName = classNameFromType(method.inputType)
            val outputClassName = classNameFromType(method.outputType)
            if (method.isClientStreaming && method.isServerStreaming) {
                val streamingBuilder = FunSpec.builder(method.name.lowerCamelCase())
                    .addKdoc(sourceInfo.comment())
                    .addModifiers(KModifier.ABSTRACT)
                    .addModifiers(KModifier.SUSPEND)
                    .addParameter(headerParameterSpec)
                    .returns(
                        BidirectionalStreamInterface::class.asClassName()
                            .parameterizedBy(inputClassName, outputClassName)
                    )
                functions.add(streamingBuilder.build())
            } else if (method.isServerStreaming) {
                val serverStreamingFunction = FunSpec.builder(method.name.lowerCamelCase())
                    .addKdoc(sourceInfo.comment())
                    .addModifiers(KModifier.ABSTRACT)
                    .addModifiers(KModifier.SUSPEND)
                    .addParameter(headerParameterSpec)
                    .returns(
                        ServerOnlyStreamInterface::class.asClassName().parameterizedBy(inputClassName, outputClassName)
                    )
                    .build()
                functions.add(serverStreamingFunction)
            } else if (method.isClientStreaming) {
                val clientStreamingFunction = FunSpec.builder(method.name.lowerCamelCase())
                    .addKdoc(sourceInfo.comment())
                    .addModifiers(KModifier.ABSTRACT)
                    .addModifiers(KModifier.SUSPEND)
                    .addParameter(headerParameterSpec)
                    .returns(
                        ClientOnlyStreamInterface::class.asClassName().parameterizedBy(inputClassName, outputClassName)
                    )
                    .build()
                functions.add(clientStreamingFunction)
            } else {
                if (configuration.generateCoroutineMethods) {
                    val unarySuspendFunction = FunSpec.builder(method.name.lowerCamelCase())
                        .addKdoc(sourceInfo.comment())
                        .addModifiers(KModifier.ABSTRACT)
                        .addModifiers(KModifier.SUSPEND)
                        .addParameter("request", inputClassName)
                        .addParameter(headerParameterSpec)
                        .returns(ResponseMessage::class.asClassName().parameterizedBy(outputClassName))
                        .build()
                    functions.add(unarySuspendFunction)
                }
                if (configuration.generateCallbackMethods) {
                    val callbackType = LambdaTypeName.get(
                        parameters = listOf(
                            ParameterSpec(
                                "",
                                ResponseMessage::class.asTypeName().parameterizedBy(outputClassName)
                            )
                        ),
                        returnType = Unit::class.java.asTypeName()
                    )
                    val unaryCallbackFunction = FunSpec.builder(method.name.lowerCamelCase())
                        .addKdoc(sourceInfo.comment())
                        .addModifiers(KModifier.ABSTRACT)
                        .addParameter("request", inputClassName)
                        .addParameter(headerParameterSpec)
                        .addParameter("onResult", callbackType)
                        .returns(CANCELABLE_CLASS_NAME)
                        .build()
                    functions.add(unaryCallbackFunction)
                }
            }
        }
        return functions
    }

    private fun serviceClientImplementation(
        javaPackageName: String,
        service: Descriptors.ServiceDescriptor,
        sourceInfo: SourceInfo
    ): TypeSpec {
        // The javaPackageName is used instead of the package name for imports and code references.
        val classBuilder = TypeSpec.classBuilder(serviceClientImplementationClassName(javaPackageName, service))
            .addSuperinterface(serviceClientInterfaceClassName(javaPackageName, service))
            .primaryConstructor(
                FunSpec.constructorBuilder()
                    .addParameter("client", ProtocolClientInterface::class)
                    .build()
            )
            .addProperty(
                PropertySpec.builder("client", ProtocolClientInterface::class, KModifier.PRIVATE)
                    .initializer("client")
                    .build()
            )
        val functionSpecs = implementationMethods(service.methods, sourceInfo)
        return classBuilder
            .addKdoc(sourceInfo.comment())
            .addFunctions(functionSpecs)
            .build()
    }

    private fun implementationMethods(
        methods: List<Descriptors.MethodDescriptor>,
        baseSourceInfo: SourceInfo
    ): List<FunSpec> {
        val functions = mutableListOf<FunSpec>()
        for ((sourceInfo, method) in methods.withSourceInfo(
            baseSourceInfo,
            DescriptorProtos.ServiceDescriptorProto.METHOD_FIELD_NUMBER
        )) {
            val inputClassName = classNameFromType(method.inputType)
            val outputClassName = classNameFromType(method.outputType)
            val methodSpecBuilder = CodeBlock.builder()
                .addStatement("MethodSpec(")
                .addStatement("\"${method.service.fullName}/${method.name}\",")
                .indent()
                .addStatement("$inputClassName::class,")
                .addStatement("$outputClassName::class,")
            if (!method.isClientStreaming && !method.isServerStreaming) {
                if (method.options.idempotencyLevel == DescriptorProtos.MethodOptions.IdempotencyLevel.NO_SIDE_EFFECTS) {
                    methodSpecBuilder.addStatement("NO_SIDE_EFFECTS")
                }
            }
            val methodSpecCallBlock = methodSpecBuilder
                .unindent()
                .addStatement("),")
                .build()
            if (method.isClientStreaming && method.isServerStreaming) {
                val streamingFunction = FunSpec.builder(method.name.lowerCamelCase())
                    .addKdoc(sourceInfo.comment())
                    .addModifiers(KModifier.OVERRIDE)
                    .addModifiers(KModifier.SUSPEND)
                    .addParameter("headers", HEADERS_CLASS_NAME)
                    .returns(
                        BidirectionalStreamInterface::class.asClassName()
                            .parameterizedBy(
                                inputClassName,
                                outputClassName
                            )
                    )
                    .addStatement(
                        "return %L",
                        CodeBlock.builder()
                            .addStatement("client.stream(")
                            .indent()
                            .addStatement("headers,")
                            .add(methodSpecCallBlock)
                            .unindent()
                            .addStatement(")")
                            .build()
                    )
                    .build()
                functions.add(streamingFunction)
            } else if (method.isServerStreaming) {
                val serverStreamingFunction = FunSpec.builder(method.name.lowerCamelCase())
                    .addKdoc(sourceInfo.comment())
                    .addModifiers(KModifier.OVERRIDE)
                    .addModifiers(KModifier.SUSPEND)
                    .addParameter("headers", HEADERS_CLASS_NAME)
                    .returns(
                        ServerOnlyStreamInterface::class.asClassName().parameterizedBy(inputClassName, outputClassName)
                    )
                    .addStatement(
                        "return %L",
                        CodeBlock.builder()
                            .addStatement("client.serverStream(")
                            .indent()
                            .addStatement("headers,")
                            .add(methodSpecCallBlock)
                            .unindent()
                            .addStatement(")")
                            .build()
                    )
                    .build()
                functions.add(serverStreamingFunction)
            } else if (method.isClientStreaming) {
                val clientStreamingFunction = FunSpec.builder(method.name.lowerCamelCase())
                    .addKdoc(sourceInfo.comment())
                    .addModifiers(KModifier.OVERRIDE)
                    .addModifiers(KModifier.SUSPEND)
                    .addParameter("headers", HEADERS_CLASS_NAME)
                    .returns(
                        ClientOnlyStreamInterface::class.asClassName().parameterizedBy(inputClassName, outputClassName)
                    )
                    .addStatement(
                        "return %L",
                        CodeBlock.builder()
                            .addStatement("client.clientStream(")
                            .indent()
                            .addStatement("headers,")
                            .add(methodSpecCallBlock)
                            .unindent()
                            .addStatement(")")
                            .build()
                    )
                    .build()
                functions.add(clientStreamingFunction)
            } else {
                if (configuration.generateCoroutineMethods) {
                    val unarySuspendFunction = FunSpec.builder(method.name.lowerCamelCase())
                        .addKdoc(sourceInfo.comment())
                        .addModifiers(KModifier.SUSPEND)
                        .addModifiers(KModifier.OVERRIDE)
                        .addParameter("request", inputClassName)
                        .addParameter("headers", HEADERS_CLASS_NAME)
                        .returns(ResponseMessage::class.asClassName().parameterizedBy(outputClassName))
                        .addStatement(
                            "return %L",
                            CodeBlock.builder()
                                .addStatement("client.unary(")
                                .indent()
                                .addStatement("request,")
                                .addStatement("headers,")
                                .add(methodSpecCallBlock)
                                .unindent()
                                .addStatement(")")
                                .build()
                        )
                        .build()
                    functions.add(unarySuspendFunction)
                }
                if (configuration.generateCallbackMethods) {
                    val callbackType = LambdaTypeName.get(
                        parameters = listOf(
                            ParameterSpec(
                                "",
                                ResponseMessage::class.asTypeName().parameterizedBy(outputClassName)
                            )
                        ),
                        returnType = Unit::class.java.asTypeName()
                    )
                    val unaryCallbackFunction = FunSpec.builder(method.name.lowerCamelCase())
                        .addKdoc(sourceInfo.comment())
                        .addModifiers(KModifier.OVERRIDE)
                        .addParameter("request", inputClassName)
                        .addParameter("headers", HEADERS_CLASS_NAME)
                        .addParameter("onResult", callbackType)
                        .returns(CANCELABLE_CLASS_NAME)
                        .addStatement(
                            "return %L",
                            CodeBlock.builder()
                                .addStatement("client.unary(")
                                .indent()
                                .addStatement("request,")
                                .addStatement("headers,")
                                .add(methodSpecCallBlock)
                                .addStatement("onResult")
                                .unindent()
                                .addStatement(")")
                                .build()
                        )
                        .build()
                    functions.add(unaryCallbackFunction)
                }
            }
        }
        return functions
    }

    private fun classNameFromType(descriptor: Descriptors.Descriptor): ClassName {
        // Get the package of the descriptor's file.
        // e.g. "build.buf.connect".
        val packageName = getFileJavaPackage(descriptor.file)
        // Get the fully qualified class name of the descriptor
        // and subtract the file's package.
        // e.g. "build.buf.connect.EmptyMessage.InnerMessage"
        // becomes ["EmptyMessage", "InnerMessage"].
        val names = getClassName(descriptor)
            .removePrefix(packageName)
            .removePrefix(".")
            .split(".")
        // Case when there is a nested entity.
        // e.g Nested message definitions and messages within "*OuterClass.java".
        if (names.size > 1) {
            return ClassName(packageName, names.first(), *names.subList(1, names.size).toTypedArray())
        }
        return ClassName(packageName, names.first())
    }
}

private fun serviceClientInterfaceClassName(packageName: String, service: Descriptors.ServiceDescriptor): ClassName {
    return ClassName(packageName, "${service.name}ClientInterface")
}

private fun serviceClientImplementationClassName(
    packageName: String,
    service: Descriptors.ServiceDescriptor
): ClassName {
    return ClassName(packageName, "${service.name}Client")
}

private fun String.lowerCamelCase(): String {
    return replaceFirstChar { char -> char.lowercaseChar() }
}

private fun String.packageToDirectory(): String {
    val dir = replace('.', '/')
    if (get(0) == '/') {
        return dir.substring(1)
    }
    return dir
}

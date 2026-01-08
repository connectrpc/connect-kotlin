// Copyright 2022-2025 The Connect Authors
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

package com.connectrpc.protocgen.connect

import com.connectrpc.BidirectionalStreamInterface
import com.connectrpc.ClientOnlyStreamInterface
import com.connectrpc.ConnectException
import com.connectrpc.Code
import com.connectrpc.Idempotency
import com.connectrpc.MethodSpec
import com.connectrpc.ProtocolClientInterface
import com.connectrpc.ResponseMessage
import com.connectrpc.ServerOnlyStreamInterface
import com.connectrpc.StreamType
import com.connectrpc.UnaryBlockingCall
import com.connectrpc.server.HandlerSpec
import com.connectrpc.server.ResponseStream
import com.connectrpc.server.ServerContext
import com.connectrpc.server.ServiceHandler
import com.connectrpc.server.UnaryHandler
import com.connectrpc.server.ServerStreamHandler
import com.connectrpc.protocgen.connect.internal.CodeGenerator
import com.connectrpc.protocgen.connect.internal.Configuration
import com.connectrpc.protocgen.connect.internal.Plugin
import com.connectrpc.protocgen.connect.internal.SourceInfo
import com.connectrpc.protocgen.connect.internal.getClassName
import com.connectrpc.protocgen.connect.internal.getFileJavaPackage
import com.connectrpc.protocgen.connect.internal.parse
import com.connectrpc.protocgen.connect.internal.withSourceInfo
import com.google.protobuf.DescriptorProtos
import com.google.protobuf.DescriptorProtos.FileDescriptorProto
import com.google.protobuf.DescriptorProtos.MethodOptions.IdempotencyLevel
import com.google.protobuf.Descriptors
import com.google.protobuf.compiler.PluginProtos
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.STAR
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName

/*
 * These are constants since com.connectrpc.Headers and com.connectrpc.http.Cancelable
 * are type aliases which doesn't have an underlying class for KotlinPoet to know what to do.
 *
 * The conventional and nicer way is to use the class type: Headers::class.asClassType() but
 * type aliasing does not allow for that.
 *
 * Instead, this is the way to reference these objects for now. If there is ever a desire to
 * move off of type aliases, this can be changed without user API breakage.
 */
private val HEADERS_CLASS_NAME = ClassName("com.connectrpc", "Headers")
private val CANCELABLE_CLASS_NAME = ClassName("com.connectrpc.http", "Cancelable")

class Generator : CodeGenerator {
    private lateinit var descriptorSource: Plugin.DescriptorSource
    private lateinit var configuration: Configuration
    private val protoFileMap = mutableMapOf<String, FileDescriptorProto>()

    override fun generate(
        request: PluginProtos.CodeGeneratorRequest,
        descriptorSource: Plugin.DescriptorSource,
        response: Plugin.Response,
    ) {
        this.descriptorSource = descriptorSource
        configuration = parse(request.parameter)
        for (protoFile in request.protoFileList) {
            protoFileMap[protoFile.name] = protoFile
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

    override fun getSupportedFeatures(): Array<PluginProtos.CodeGeneratorResponse.Feature> {
        return arrayOf(
            PluginProtos.CodeGeneratorResponse.Feature.FEATURE_PROTO3_OPTIONAL,
            PluginProtos.CodeGeneratorResponse.Feature.FEATURE_SUPPORTS_EDITIONS,
        )
    }

    override fun getMinimumEdition(): DescriptorProtos.Edition {
        return DescriptorProtos.Edition.EDITION_PROTO2
    }

    override fun getMaximumEdition(): DescriptorProtos.Edition {
        return DescriptorProtos.Edition.EDITION_2023
    }

    private fun parseFile(file: Descriptors.FileDescriptor): Map<ClassName, FileSpec> {
        val baseSourceInfo = SourceInfo(protoFileMap[file.name]!!, descriptorSource, emptyList())
        val fileSpecs = mutableMapOf<ClassName, FileSpec>()
        val packageName = getFileJavaPackage(file)
        for ((sourceInfo, service) in file.services.withSourceInfo(
            baseSourceInfo,
            FileDescriptorProto.SERVICE_FIELD_NUMBER,
        )) {
            val interfaceFileSpec = FileSpec.builder(packageName, file.name)
                .addFileComment("Code generated by connect-kotlin. DO NOT EDIT.\n")
                .addFileComment("\n")
                .addFileComment("Source: ${file.name}\n")
                .suppressDeprecationWarnings(file)
                .addType(serviceClientInterface(packageName, service, file, sourceInfo))
                .build()
            fileSpecs[serviceClientInterfaceClassName(packageName, service)] = interfaceFileSpec

            val implementationFileSpecBuilder = FileSpec.builder(packageName, file.name)
                .addImport(MethodSpec::class.java.`package`.name, "MethodSpec")
                .addImport(StreamType::class.java.`package`.name, "StreamType")
                .addFileComment("Code generated by connect-kotlin. DO NOT EDIT.\n")
                .addFileComment("\n")
                .addFileComment("Source: ${file.name}\n")
                .suppressDeprecationWarnings(file)
                // Set the file package for the generated methods.
                .addType(serviceClientImplementation(packageName, service, file, sourceInfo))
            for (method in service.methods) {
                if (method.options.hasIdempotencyLevel()) {
                    implementationFileSpecBuilder.addImport(Idempotency::class.java.`package`.name, "Idempotency")
                    break
                }
            }
            val implementationFileSpec = implementationFileSpecBuilder.build()
            fileSpecs[serviceClientImplementationClassName(packageName, service)] = implementationFileSpec

            // Generate server code if enabled
            if (configuration.generateServerMethods) {
                val serverInterfaceFileSpec = FileSpec.builder(packageName, file.name)
                    .addFileComment("Code generated by connect-kotlin. DO NOT EDIT.\n")
                    .addFileComment("\n")
                    .addFileComment("Source: ${file.name}\n")
                    .suppressDeprecationWarnings(file)
                    .addType(serviceServerInterface(packageName, service, file, sourceInfo))
                    .build()
                fileSpecs[serviceServerInterfaceClassName(packageName, service)] = serverInterfaceFileSpec

                val unimplementedFileSpecBuilder = FileSpec.builder(packageName, file.name)
                    .addImport(ConnectException::class.java.`package`.name, "ConnectException")
                    .addImport(Code::class.java.`package`.name, "Code")
                    .addFileComment("Code generated by connect-kotlin. DO NOT EDIT.\n")
                    .addFileComment("\n")
                    .addFileComment("Source: ${file.name}\n")
                    .suppressDeprecationWarnings(file)
                    .addType(unimplementedServiceHandler(packageName, service, file, sourceInfo))
                val unimplementedFileSpec = unimplementedFileSpecBuilder.build()
                fileSpecs[unimplementedServiceHandlerClassName(packageName, service)] = unimplementedFileSpec

                val handlerSpecsFileSpec = FileSpec.builder(packageName, file.name)
                    .addImport(HandlerSpec::class.java.`package`.name, "HandlerSpec")
                    .addImport(UnaryHandler::class.java.`package`.name, "UnaryHandler")
                    .addImport(ServerStreamHandler::class.java.`package`.name, "ServerStreamHandler")
                    .addImport(StreamType::class.java.`package`.name, "StreamType")
                    .addFileComment("Code generated by connect-kotlin. DO NOT EDIT.\n")
                    .addFileComment("\n")
                    .addFileComment("Source: ${file.name}\n")
                    .suppressDeprecationWarnings(file)
                    .addFunction(asHandlerSpecsFunction(packageName, service, sourceInfo))
                for (method in service.methods) {
                    if (method.options.hasIdempotencyLevel()) {
                        handlerSpecsFileSpec.addImport(Idempotency::class.java.`package`.name, "Idempotency")
                        break
                    }
                }
                fileSpecs[handlerSpecsClassName(packageName, service)] = handlerSpecsFileSpec.build()
            }
        }
        return fileSpecs
    }

    private fun serviceClientInterface(
        packageName: String,
        service: Descriptors.ServiceDescriptor,
        file: Descriptors.FileDescriptor,
        sourceInfo: SourceInfo,
    ): TypeSpec {
        val interfaceBuilder = TypeSpec.interfaceBuilder(serviceClientInterfaceClassName(packageName, service))
        val functionSpecs = interfaceMethods(service.methods, sourceInfo)
        return interfaceBuilder
            .addServiceDeprecation(service, file)
            .addKdoc(sourceInfo.comment().sanitizeKdoc())
            .addFunctions(functionSpecs)
            .build()
    }

    private fun interfaceMethods(
        methods: List<Descriptors.MethodDescriptor>,
        baseSourceInfo: SourceInfo,
    ): List<FunSpec> {
        val functions = mutableListOf<FunSpec>()
        val headerParameterSpec = ParameterSpec.builder("headers", HEADERS_CLASS_NAME)
            .defaultValue("%L", "emptyMap()")
            .build()
        for ((sourceInfo, method) in methods.withSourceInfo(
            baseSourceInfo,
            DescriptorProtos.ServiceDescriptorProto.METHOD_FIELD_NUMBER,
        )) {
            val inputClassName = classNameFromType(method.inputType)
            val outputClassName = classNameFromType(method.outputType)
            if (method.isClientStreaming && method.isServerStreaming) {
                val streamingBuilder = FunSpec.builder(method.name.lowerCamelCase())
                    .addKdoc(sourceInfo.comment().sanitizeKdoc())
                    .addMethodDeprecation(method)
                    .addModifiers(KModifier.ABSTRACT)
                    .addModifiers(KModifier.SUSPEND)
                    .addParameter(headerParameterSpec)
                    .returns(
                        BidirectionalStreamInterface::class.asClassName()
                            .parameterizedBy(inputClassName, outputClassName),
                    )
                functions.add(streamingBuilder.build())
            } else if (method.isServerStreaming) {
                val serverStreamingFunction = FunSpec.builder(method.name.lowerCamelCase())
                    .addKdoc(sourceInfo.comment().sanitizeKdoc())
                    .addMethodDeprecation(method)
                    .addModifiers(KModifier.ABSTRACT)
                    .addModifiers(KModifier.SUSPEND)
                    .addParameter(headerParameterSpec)
                    .returns(
                        ServerOnlyStreamInterface::class.asClassName().parameterizedBy(inputClassName, outputClassName),
                    )
                    .build()
                functions.add(serverStreamingFunction)
            } else if (method.isClientStreaming) {
                val clientStreamingFunction = FunSpec.builder(method.name.lowerCamelCase())
                    .addKdoc(sourceInfo.comment().sanitizeKdoc())
                    .addMethodDeprecation(method)
                    .addModifiers(KModifier.ABSTRACT)
                    .addModifiers(KModifier.SUSPEND)
                    .addParameter(headerParameterSpec)
                    .returns(
                        ClientOnlyStreamInterface::class.asClassName().parameterizedBy(inputClassName, outputClassName),
                    )
                    .build()
                functions.add(clientStreamingFunction)
            } else {
                if (configuration.generateCoroutineMethods) {
                    val unarySuspendFunction = FunSpec.builder(method.name.lowerCamelCase())
                        .addKdoc(sourceInfo.comment().sanitizeKdoc())
                        .addMethodDeprecation(method)
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
                                ResponseMessage::class.asTypeName().parameterizedBy(outputClassName),
                            ),
                        ),
                        returnType = Unit::class.java.asTypeName(),
                    )
                    val unaryCallbackFunction = FunSpec.builder(method.name.lowerCamelCase())
                        .addKdoc(sourceInfo.comment().sanitizeKdoc())
                        .addMethodDeprecation(method)
                        .addModifiers(KModifier.ABSTRACT)
                        .addParameter("request", inputClassName)
                        .addParameter(headerParameterSpec)
                        .addParameter("onResult", callbackType)
                        .returns(CANCELABLE_CLASS_NAME)
                        .build()
                    functions.add(unaryCallbackFunction)
                }
                if (configuration.generateBlockingUnaryMethods) {
                    val unarySuspendFunction = FunSpec.builder("${method.name.lowerCamelCase()}Blocking")
                        .addKdoc(sourceInfo.comment().sanitizeKdoc())
                        .addModifiers(KModifier.ABSTRACT)
                        .addParameter("request", inputClassName)
                        .addParameter(headerParameterSpec)
                        .returns(UnaryBlockingCall::class.asClassName().parameterizedBy(outputClassName))
                        .build()
                    functions.add(unarySuspendFunction)
                }
            }
        }
        return functions
    }

    private fun serviceClientImplementation(
        javaPackageName: String,
        service: Descriptors.ServiceDescriptor,
        file: Descriptors.FileDescriptor,
        sourceInfo: SourceInfo,
    ): TypeSpec {
        // The javaPackageName is used instead of the package name for imports and code references.
        val classBuilder = TypeSpec.classBuilder(serviceClientImplementationClassName(javaPackageName, service))
            .addSuperinterface(serviceClientInterfaceClassName(javaPackageName, service))
            .primaryConstructor(
                FunSpec.constructorBuilder()
                    .addParameter("client", ProtocolClientInterface::class)
                    .build(),
            )
            .addProperty(
                PropertySpec.builder("client", ProtocolClientInterface::class, KModifier.PRIVATE)
                    .initializer("client")
                    .build(),
            )
        val functionSpecs = implementationMethods(service.methods, sourceInfo)
        return classBuilder
            .addKdoc(sourceInfo.comment().sanitizeKdoc())
            .addServiceDeprecation(service, file)
            .addFunctions(functionSpecs)
            .build()
    }

    private fun implementationMethods(
        methods: List<Descriptors.MethodDescriptor>,
        baseSourceInfo: SourceInfo,
    ): List<FunSpec> {
        val functions = mutableListOf<FunSpec>()
        for ((sourceInfo, method) in methods.withSourceInfo(
            baseSourceInfo,
            DescriptorProtos.ServiceDescriptorProto.METHOD_FIELD_NUMBER,
        )) {
            val inputClassName = classNameFromType(method.inputType)
            val outputClassName = classNameFromType(method.outputType)
            val methodSpecBuilder = CodeBlock.builder()
                .addStatement("MethodSpec(")
                .addStatement("\"${method.service.fullName}/${method.name}\",")
                .indent()
                .addStatement("$inputClassName::class,")
                .addStatement("$outputClassName::class,")
            if (method.isClientStreaming && method.isServerStreaming) {
                methodSpecBuilder.addStatement("StreamType.${StreamType.BIDI.name},")
            } else if (method.isClientStreaming) {
                methodSpecBuilder.addStatement("StreamType.${StreamType.CLIENT.name},")
            } else if (method.isServerStreaming) {
                methodSpecBuilder.addStatement("StreamType.${StreamType.SERVER.name},")
            } else {
                methodSpecBuilder.addStatement("StreamType.${StreamType.UNARY.name},")
            }
            when (method.options.idempotencyLevel) {
                IdempotencyLevel.NO_SIDE_EFFECTS -> methodSpecBuilder.addStatement("idempotency = Idempotency.${Idempotency.NO_SIDE_EFFECTS.name},")

                IdempotencyLevel.IDEMPOTENT -> methodSpecBuilder.addStatement("idempotency = Idempotency.${Idempotency.IDEMPOTENT.name},")

                else -> {
                    // Use default value in method spec.
                }
            }
            val methodSpecCallBlock = methodSpecBuilder
                .unindent()
                .addStatement("),")
                .build()
            if (method.isClientStreaming && method.isServerStreaming) {
                val streamingFunction = FunSpec.builder(method.name.lowerCamelCase())
                    .addKdoc(sourceInfo.comment().sanitizeKdoc())
                    .addMethodDeprecation(method)
                    .addModifiers(KModifier.OVERRIDE)
                    .addModifiers(KModifier.SUSPEND)
                    .addParameter("headers", HEADERS_CLASS_NAME)
                    .returns(
                        BidirectionalStreamInterface::class.asClassName()
                            .parameterizedBy(
                                inputClassName,
                                outputClassName,
                            ),
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
                            .build(),
                    )
                    .build()
                functions.add(streamingFunction)
            } else if (method.isServerStreaming) {
                val serverStreamingFunction = FunSpec.builder(method.name.lowerCamelCase())
                    .addKdoc(sourceInfo.comment().sanitizeKdoc())
                    .addMethodDeprecation(method)
                    .addModifiers(KModifier.OVERRIDE)
                    .addModifiers(KModifier.SUSPEND)
                    .addParameter("headers", HEADERS_CLASS_NAME)
                    .returns(
                        ServerOnlyStreamInterface::class.asClassName().parameterizedBy(inputClassName, outputClassName),
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
                            .build(),
                    )
                    .build()
                functions.add(serverStreamingFunction)
            } else if (method.isClientStreaming) {
                val clientStreamingFunction = FunSpec.builder(method.name.lowerCamelCase())
                    .addKdoc(sourceInfo.comment().sanitizeKdoc())
                    .addMethodDeprecation(method)
                    .addModifiers(KModifier.OVERRIDE)
                    .addModifiers(KModifier.SUSPEND)
                    .addParameter("headers", HEADERS_CLASS_NAME)
                    .returns(
                        ClientOnlyStreamInterface::class.asClassName().parameterizedBy(inputClassName, outputClassName),
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
                            .build(),
                    )
                    .build()
                functions.add(clientStreamingFunction)
            } else {
                if (configuration.generateCoroutineMethods) {
                    val unarySuspendFunction = FunSpec.builder(method.name.lowerCamelCase())
                        .addKdoc(sourceInfo.comment().sanitizeKdoc())
                        .addMethodDeprecation(method)
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
                                .build(),
                        )
                        .build()
                    functions.add(unarySuspendFunction)
                }
                if (configuration.generateCallbackMethods) {
                    val callbackType = LambdaTypeName.get(
                        parameters = listOf(
                            ParameterSpec(
                                "",
                                ResponseMessage::class.asTypeName().parameterizedBy(outputClassName),
                            ),
                        ),
                        returnType = Unit::class.java.asTypeName(),
                    )
                    val unaryCallbackFunction = FunSpec.builder(method.name.lowerCamelCase())
                        .addKdoc(sourceInfo.comment().sanitizeKdoc())
                        .addMethodDeprecation(method)
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
                                .build(),
                        )
                        .build()
                    functions.add(unaryCallbackFunction)
                }
                if (configuration.generateBlockingUnaryMethods) {
                    val unarySuspendFunction = FunSpec.builder("${method.name.lowerCamelCase()}Blocking")
                        .addKdoc(sourceInfo.comment().sanitizeKdoc())
                        .addModifiers(KModifier.OVERRIDE)
                        .addParameter("request", inputClassName)
                        .addParameter("headers", HEADERS_CLASS_NAME)
                        .returns(UnaryBlockingCall::class.asClassName().parameterizedBy(outputClassName))
                        .addStatement(
                            "return %L",
                            CodeBlock.builder()
                                .addStatement("client.unaryBlocking(")
                                .indent()
                                .addStatement("request,")
                                .addStatement("headers,")
                                .add(methodSpecCallBlock)
                                .unindent()
                                .addStatement(")")
                                .build(),
                        )
                        .build()
                    functions.add(unarySuspendFunction)
                }
            }
        }
        return functions
    }

    private fun classNameFromType(descriptor: Descriptors.Descriptor): ClassName {
        // Get the package of the descriptor's file.
        // e.g. "com.connectrpc".
        val packageName = getFileJavaPackage(descriptor.file)
        // Get the fully qualified class name of the descriptor
        // and subtract the file's package.
        // e.g. "com.connectrpc.EmptyMessage.InnerMessage"
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

    private fun String.sanitizeKdoc(): String {
        return this
            // Remove trailing whitespace on each line.
            .replace("[^\\S\n]+\n".toRegex(), "\n")
            .replace("\\s+$".toRegex(), "")
            .replace("\\*/".toRegex(), "&#42;/")
            .replace("/\\*".toRegex(), "/&#42;")
            .replace("""[""", "&#91;")
            .replace("""]""", "&#93;")
            .replace("@", "&#64;")
            .replace("%", "%%")
    }

    // Server-side code generation

    private fun serviceServerInterface(
        packageName: String,
        service: Descriptors.ServiceDescriptor,
        file: Descriptors.FileDescriptor,
        sourceInfo: SourceInfo,
    ): TypeSpec {
        val interfaceBuilder = TypeSpec.interfaceBuilder(serviceServerInterfaceClassName(packageName, service))
            .addSuperinterface(ServiceHandler::class)
        val serviceNameProperty = PropertySpec.builder("serviceName", String::class)
            .addModifiers(KModifier.OVERRIDE)
            .getter(
                FunSpec.getterBuilder()
                    .addStatement("return %S", service.fullName)
                    .build(),
            )
            .build()
        interfaceBuilder.addProperty(serviceNameProperty)
        val functionSpecs = serverInterfaceMethods(service.methods, sourceInfo)
        return interfaceBuilder
            .addServiceDeprecation(service, file)
            .addKdoc(sourceInfo.comment().sanitizeKdoc())
            .addFunctions(functionSpecs)
            .build()
    }

    private fun serverInterfaceMethods(
        methods: List<Descriptors.MethodDescriptor>,
        baseSourceInfo: SourceInfo,
    ): List<FunSpec> {
        val functions = mutableListOf<FunSpec>()
        for ((sourceInfo, method) in methods.withSourceInfo(
            baseSourceInfo,
            DescriptorProtos.ServiceDescriptorProto.METHOD_FIELD_NUMBER,
        )) {
            val inputClassName = classNameFromType(method.inputType)
            val outputClassName = classNameFromType(method.outputType)
            val ctxParam = ParameterSpec.builder("ctx", ServerContext::class).build()
            val requestParam = ParameterSpec.builder("request", inputClassName).build()

            if (method.isClientStreaming && method.isServerStreaming) {
                // Bidi streaming - not yet supported
                continue
            } else if (method.isServerStreaming) {
                val responseStreamType = ResponseStream::class.asClassName().parameterizedBy(outputClassName)
                val serverStreamingFunction = FunSpec.builder(method.name.lowerCamelCase())
                    .addKdoc(sourceInfo.comment().sanitizeKdoc())
                    .addMethodDeprecation(method)
                    .addModifiers(KModifier.ABSTRACT)
                    .addModifiers(KModifier.SUSPEND)
                    .addParameter(ctxParam)
                    .addParameter(requestParam)
                    .addParameter("responses", responseStreamType)
                    .build()
                functions.add(serverStreamingFunction)
            } else if (method.isClientStreaming) {
                // Client streaming - not yet supported
                continue
            } else {
                val unaryFunction = FunSpec.builder(method.name.lowerCamelCase())
                    .addKdoc(sourceInfo.comment().sanitizeKdoc())
                    .addMethodDeprecation(method)
                    .addModifiers(KModifier.ABSTRACT)
                    .addModifiers(KModifier.SUSPEND)
                    .addParameter(ctxParam)
                    .addParameter(requestParam)
                    .returns(outputClassName)
                    .build()
                functions.add(unaryFunction)
            }
        }
        return functions
    }

    private fun unimplementedServiceHandler(
        packageName: String,
        service: Descriptors.ServiceDescriptor,
        file: Descriptors.FileDescriptor,
        sourceInfo: SourceInfo,
    ): TypeSpec {
        val classBuilder = TypeSpec.classBuilder(unimplementedServiceHandlerClassName(packageName, service))
            .addModifiers(KModifier.OPEN)
            .addSuperinterface(serviceServerInterfaceClassName(packageName, service))
        val serviceNameProperty = PropertySpec.builder("serviceName", String::class)
            .addModifiers(KModifier.OVERRIDE)
            .getter(
                FunSpec.getterBuilder()
                    .addStatement("return %S", service.fullName)
                    .build(),
            )
            .build()
        classBuilder.addProperty(serviceNameProperty)
        val functionSpecs = unimplementedMethods(service.methods, sourceInfo)
        return classBuilder
            .addServiceDeprecation(service, file)
            .addKdoc(sourceInfo.comment().sanitizeKdoc())
            .addFunctions(functionSpecs)
            .build()
    }

    private fun unimplementedMethods(
        methods: List<Descriptors.MethodDescriptor>,
        baseSourceInfo: SourceInfo,
    ): List<FunSpec> {
        val functions = mutableListOf<FunSpec>()
        for ((sourceInfo, method) in methods.withSourceInfo(
            baseSourceInfo,
            DescriptorProtos.ServiceDescriptorProto.METHOD_FIELD_NUMBER,
        )) {
            val inputClassName = classNameFromType(method.inputType)
            val outputClassName = classNameFromType(method.outputType)
            val ctxParam = ParameterSpec.builder("ctx", ServerContext::class).build()
            val requestParam = ParameterSpec.builder("request", inputClassName).build()

            if (method.isClientStreaming && method.isServerStreaming) {
                continue
            } else if (method.isServerStreaming) {
                val responseStreamType = ResponseStream::class.asClassName().parameterizedBy(outputClassName)
                val serverStreamingFunction = FunSpec.builder(method.name.lowerCamelCase())
                    .addKdoc(sourceInfo.comment().sanitizeKdoc())
                    .addMethodDeprecation(method)
                    .addModifiers(KModifier.OVERRIDE)
                    .addModifiers(KModifier.SUSPEND)
                    .addParameter(ctxParam)
                    .addParameter(requestParam)
                    .addParameter("responses", responseStreamType)
                    .addStatement(
                        "throw %T(%T.UNIMPLEMENTED, %S)",
                        ConnectException::class,
                        Code::class,
                        "${method.service.fullName}/${method.name} is not implemented",
                    )
                    .build()
                functions.add(serverStreamingFunction)
            } else if (method.isClientStreaming) {
                continue
            } else {
                val unaryFunction = FunSpec.builder(method.name.lowerCamelCase())
                    .addKdoc(sourceInfo.comment().sanitizeKdoc())
                    .addMethodDeprecation(method)
                    .addModifiers(KModifier.OVERRIDE)
                    .addModifiers(KModifier.SUSPEND)
                    .addParameter(ctxParam)
                    .addParameter(requestParam)
                    .returns(outputClassName)
                    .addStatement(
                        "throw %T(%T.UNIMPLEMENTED, %S)",
                        ConnectException::class,
                        Code::class,
                        "${method.service.fullName}/${method.name} is not implemented",
                    )
                    .build()
                functions.add(unaryFunction)
            }
        }
        return functions
    }

    private fun asHandlerSpecsFunction(
        packageName: String,
        service: Descriptors.ServiceDescriptor,
        baseSourceInfo: SourceInfo,
    ): FunSpec {
        val handlerSpecListType = List::class.asClassName().parameterizedBy(
            HandlerSpec::class.asClassName().parameterizedBy(STAR, STAR),
        )
        val funBuilder = FunSpec.builder("asHandlerSpecs")
            .receiver(serviceServerInterfaceClassName(packageName, service))
            .returns(handlerSpecListType)

        val codeBlock = CodeBlock.builder()
            .addStatement("return listOf(")
            .indent()

        for ((_, method) in service.methods.withSourceInfo(
            baseSourceInfo,
            DescriptorProtos.ServiceDescriptorProto.METHOD_FIELD_NUMBER,
        )) {
            if (method.isClientStreaming) continue // Skip unsupported

            val inputClassName = classNameFromType(method.inputType)
            val outputClassName = classNameFromType(method.outputType)

            codeBlock.addStatement("HandlerSpec(")
            codeBlock.indent()
            codeBlock.addStatement("%S,", "${method.service.fullName}/${method.name}")
            codeBlock.addStatement("$inputClassName::class,")
            codeBlock.addStatement("$outputClassName::class,")

            if (method.isServerStreaming) {
                codeBlock.addStatement("StreamType.SERVER,")
                codeBlock.addStatement("ServerStreamHandler { ctx, req, resp -> this.${method.name.lowerCamelCase()}(ctx, req, resp) },")
            } else {
                codeBlock.addStatement("StreamType.UNARY,")
                codeBlock.addStatement("UnaryHandler { ctx, req -> this.${method.name.lowerCamelCase()}(ctx, req) },")
            }

            when (method.options.idempotencyLevel) {
                IdempotencyLevel.NO_SIDE_EFFECTS -> codeBlock.addStatement("idempotency = Idempotency.NO_SIDE_EFFECTS,")
                IdempotencyLevel.IDEMPOTENT -> codeBlock.addStatement("idempotency = Idempotency.IDEMPOTENT,")
                else -> {}
            }

            codeBlock.unindent()
            codeBlock.addStatement("),")
        }

        codeBlock.unindent()
        codeBlock.addStatement(")")

        funBuilder.addCode(codeBlock.build())
        return funBuilder.build()
    }
}

private fun serviceClientInterfaceClassName(packageName: String, service: Descriptors.ServiceDescriptor): ClassName {
    return ClassName(packageName, "${service.name}ClientInterface")
}

private fun serviceClientImplementationClassName(
    packageName: String,
    service: Descriptors.ServiceDescriptor,
): ClassName {
    return ClassName(packageName, "${service.name}Client")
}

private fun serviceServerInterfaceClassName(packageName: String, service: Descriptors.ServiceDescriptor): ClassName {
    return ClassName(packageName, "${service.name}ServerInterface")
}

private fun unimplementedServiceHandlerClassName(
    packageName: String,
    service: Descriptors.ServiceDescriptor,
): ClassName {
    return ClassName(packageName, "Unimplemented${service.name}Server")
}

private fun handlerSpecsClassName(
    packageName: String,
    service: Descriptors.ServiceDescriptor,
): ClassName {
    return ClassName(packageName, "${service.name}ServerHandlerSpecs")
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

private fun TypeSpec.Builder.addServiceDeprecation(
    service: Descriptors.ServiceDescriptor,
    file: Descriptors.FileDescriptor,
): TypeSpec.Builder {
    if (service.options.deprecated) {
        this.addAnnotation(
            AnnotationSpec.builder(Deprecated::class)
                .addMember("%S", "The service is deprecated in the Protobuf source file.")
                .build(),
        )
    } else if (file.options.deprecated) {
        this.addAnnotation(
            AnnotationSpec.builder(Deprecated::class)
                .addMember("%S", "The Protobuf source file that defines this service is deprecated.")
                .build(),
        )
    }
    return this
}

private fun FunSpec.Builder.addMethodDeprecation(
    method: Descriptors.MethodDescriptor,
): FunSpec.Builder {
    if (method.options.deprecated) {
        this.addAnnotation(
            AnnotationSpec.builder(Deprecated::class)
                .addMember("%S", "The method is deprecated in the Protobuf source file.")
                .build(),
        )
    }
    return this
}

private fun FileSpec.Builder.suppressDeprecationWarnings(
    file: Descriptors.FileDescriptor,
): FileSpec.Builder {
    val hasDeprecated = file.options.deprecated || file.services.find { s -> s.options.deprecated || s.methods.find { m -> m.options.deprecated } != null } != null
    if (hasDeprecated) {
        this.addAnnotation(
            AnnotationSpec.builder(Suppress::class)
                .addMember("%S", "DEPRECATION")
                .build(),
        )
    }
    return this
}

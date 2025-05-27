[![Build](https://github.com/connectrpc/connect-kotlin/actions/workflows/ci.yml/badge.svg)](https://github.com/connectrpc/connect-kotlin/actions/workflows/ci.yml)
[![License](https://img.shields.io/github/license/connectrpc/connect-kotlin.svg?color=blue&label=License)](https://opensource.org/licenses/Apache-2.0)

Connect-Kotlin
==============

Connect-Kotlin is a slim library for using generated, type-safe, and idiomatic 
Kotlin clients to communicate with your app's servers using [Protocol Buffers (Protobuf)][protobuf]. 
It works with the [Connect][connect-protocol], [gRPC][grpc-protocol], and
[gRPC-Web][grpc-web-protocol] protocols.

Given a simple Protobuf schema, Connect-Kotlin generates idiomatic Kotlin
protocol interfaces and client implementations:

<details><summary>Click to expand <code>ElizaServiceClient.kt</code></summary>

```kotlin
public class ElizaServiceClient(
    private val client: ProtocolClientInterface
) : ElizaServiceClientInterface {
    public override suspend fun say(request: SayRequest, headers: Headers):
        ResponseMessage<SayResponse> = client.unary(
        request,
        headers,
        MethodSpec(
            "connectrpc.eliza.v1.ElizaService/Say",
            com.connectrpc.eliza.v1.SayRequest::class,
            com.connectrpc.eliza.v1.SayResponse::class
        )
    )

    public override suspend fun converse(headers: Headers):
        BidirectionalStreamInterface<ConverseRequest, ConverseResponse> = client.stream(
        headers,
        MethodSpec(
            "connectrpc.eliza.v1.ElizaService/Converse",
            com.connectrpc.eliza.v1.ConverseRequest::class,
            com.connectrpc.eliza.v1.ConverseResponse::class
        )
    )

    public override suspend fun introduce(headers: Headers):
        ServerOnlyStreamInterface<IntroduceRequest, IntroduceResponse> = client.serverStream(
        headers,
        MethodSpec(
            "connectrpc.eliza.v1.ElizaService/Introduce",
            com.connectrpc.eliza.v1.IntroduceRequest::class,
            com.connectrpc.eliza.v1.IntroduceResponse::class
        )
    )
}
```

</details>
<details><summary>Click to expand <code>ElizaServiceClientInterface.kt</code></summary>

```kotlin
public interface ElizaServiceClientInterface {
    public suspend fun say(request: SayRequest, headers: Headers = emptyMap()):
        ResponseMessage<SayResponse>

    public suspend fun converse(headers: Headers = emptyMap()):
        BidirectionalStreamInterface<ConverseRequest, ConverseResponse>

    public suspend fun introduce(headers: Headers = emptyMap()):
        ServerOnlyStreamInterface<IntroduceRequest, IntroduceResponse>
}
```

</details>

This code can then be integrated with just a few lines:

```kotlin
class MainActivity : AppCompatActivity() {
    private lateinit var elizaServiceClient: ElizaServiceClientInterface

    private suspend fun say(sentence: String) {
        // Make a unary request to Eliza.
        val response = elizaServiceClient.say(SayRequest.newBuilder().setSentence(sentence).build())
        val elizaSentence = response.success { success ->
            // Get Eliza's reply from the response.
            success.message.sentence
        }
        // Use the elizaSentence in your views.
    }
}
```

That’s it! You no longer need to manually define request/response models,
specify the exact path of your request, nor worry about the underlying networking
transport for your applications!

## Quick Start

Head over to our [quick start tutorial][getting-started] to get started.
It only takes ~10 minutes to complete a working chat app that uses Connect-Kotlin!

## Documentation

Comprehensive documentation for everything, including
[interceptors][interceptors], [streaming][streaming], and [error handling][error-handling]
is available on the [connectrpc.com website][getting-started].

## Generation Options

| **Option**                     | **Type** | **Default** | **Details**                                     |
|--------------------------------|:--------:|:-----------:|-------------------------------------------------|
| `generateCallbackMethods`      | Boolean  |   `false`   | Generate callback signatures for unary methods. |
| `generateCoroutineMethods`     | Boolean  |   `true`    | Generate suspend signatures for unary methods.  |
| `generateBlockingUnaryMethods` | Boolean  |   `false`   | Generate blocking signatures for unary methods. |

## Example Apps

Example apps are available in [`/examples`](./examples). First, run `make generate` to generate
code for the Protobuf plugins.

For the [Android example](./examples/android), you can run `make installandroid` to build and install
a fully functional Android application using Connect-Kotlin.

Additionally, there are pure Kotlin examples that demonstrate a simple main executable using Connect-Kotlin:
- [`/examples/kotlin-google-java`](./examples/kotlin-google-java): A simple Kotlin main
executable using Google's Java Protobuf generator plugin
- [`/examples/kotlin-google-javalite`](./examples/kotlin-google-javalite): A simple Kotlin main
executable using Google's Javalite Protobuf generator plugin

The examples demonstrates:

- Using streaming APIs in an Android app
- Using Google's Java and Javalite generated data types
- Using the [Connect protocol][connect-protocol]
- Using the [gRPC protocol][grpc-protocol]
- Using the [gRPC-Web protocol][grpc-web-protocol]

## Contributing

We'd love your help making Connect better!

Extensive instructions for building the library and generator plugins locally,
running tests, and contributing to the repository are available in our
[`CONTRIBUTING.md` guide](./.github/CONTRIBUTING.md). Please check it out
for details.

## Ecosystem

* [connect-swift]: Swift clients for idiomatic gRPC & Connect RPC
* [connect-es]: Type-safe APIs with Protobuf and TypeScript.
* [connect-go]: Service handlers and clients for GoLang
* [Buf Studio][buf-studio]: web UI for ad-hoc RPCs
* [conformance]: Connect, gRPC, and gRPC-Web interoperability tests

## Status

This project is in beta, and we may make a few changes as we gather feedback
from early adopters. Join us on [Slack][slack]!

## Legal

Offered under the [Apache 2 license][license].

[blog]: https://buf.build/blog/connect-a-better-grpc
[buf-studio]: https://buf.build/studio
[conformance]: https://github.com/connectrpc/conformance
[connect-go]: https://github.com/connectrpc/connect-go
[connect-protocol]: https://connectrpc.com/docs/protocol
[connect-swift]: https://github.com/connectrpc/connect-swift
[connect-es]: https://www.npmjs.com/package/@connectrpc/connect
[error-handling]: https://connectrpc.com/docs/kotlin/errors
[getting-started]: https://connectrpc.com/docs/kotlin/getting-started
[grpc-protocol]: https://github.com/grpc/grpc/blob/master/doc/PROTOCOL-HTTP2.md
[grpc-web-protocol]: https://github.com/grpc/grpc/blob/master/doc/PROTOCOL-WEB.md
[interceptors]: https://connectrpc.com/docs/kotlin/interceptors
[license]: https://github.com/connectrpc/connect-kotlin/blob/main/LICENSE
[protobuf]: https://developers.google.com/protocol-buffers
[protocol]: https://connectrpc.com/docs/protocol
[server reflection]: https://github.com/connectrpc/grpcreflect-go
[slack]: https://buf.build/links/slack
[streaming]: https://connectrpc.com/docs/kotlin/using-clients#using-generated-clients

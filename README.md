![Build](https://github.com/bufbuild/connect-kotlin/actions/workflows/ci.yml/badge.svg)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

Connect-Kotlin
==============

Connect-Kotlin is a slim library for building browser and gRPC-compatible HTTP APIs.
You write a short [Protocol Buffer][protobuf] schema and implement your
application logic, and Connect generates code to handle marshaling, routing,
compression, and content type negotiation. It also generates an idiomatic,
type-safe client. Handlers and clients support three protocols: gRPC, gRPC-Web,
and Connect's own protocol.

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
            "buf.connect.demo.eliza.v1.ElizaService/Say",
            buf.connect.demo.eliza.v1.SayRequest::class,
            buf.connect.demo.eliza.v1.SayResponse::class
        )
    )

    public override suspend fun converse(headers: Headers):
        BidirectionalStreamInterface<ConverseRequest, ConverseResponse> = client.stream(
        headers,
        MethodSpec(
            "buf.connect.demo.eliza.v1.ElizaService/Converse",
            buf.connect.demo.eliza.v1.ConverseRequest::class,
            buf.connect.demo.eliza.v1.ConverseResponse::class
        )
    )

    public override suspend fun introduce(headers: Headers):
        ServerOnlyStreamInterface<IntroduceRequest, IntroduceResponse> = client.serverStream(
        headers,
        MethodSpec(
            "buf.connect.demo.eliza.v1.ElizaService/Introduce",
            buf.connect.demo.eliza.v1.IntroduceRequest::class,
            buf.connect.demo.eliza.v1.IntroduceResponse::class
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
is available on the [connect.build website][getting-started].

## Generation Options

| **Option**                 | **Type** | **Default** | **Repeatable** | **Details**                                     |
|----------------------------|:--------:|:-----------:|:--------------:|-------------------------------------------------|
| `generateCallbackMethods`  | Boolean  |   `false`   |       No       | Generate callback signatures for unary methods. |
| `generateCoroutineMethods` | Boolean  |   `true`    |       No       | Generate suspend signatures for unary methods.  |

## Example Apps

Example apps are available in [`/examples`](./examples). First, run `make generate` to generate
code for the Protobuf plugins.

For the [Android example](./examples/android), you can run `make installandroid` to build and install
a fully functional Android application using Connect-Kotlin.

Additionally, there are pure Kotlin examples that demonstrate a simple main executable using Connect-Kotlin:
- [`/examples/kotlin-google-java`](./examples/kotlin-google-java): A simple Kotlin main
executable using Google's Java Protobuf generator plugin
- [`/examples/kotlin-google-javalite`](./examples/kotlin-google-java): A simple Kotlin main
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
* [connect-web]: TypeScript clients for web browsers
* [connect-go]: Service handlers and clients for GoLang
* [Buf Studio][buf-studio]: web UI for ad-hoc RPCs
* [connect-crosstest]: gRPC and gRPC-Web interoperability tests

## Status

This project is in beta, and we may make a few changes as we gather feedback
from early adopters. Join us on [Slack][slack]!

## Legal

Offered under the [Apache 2 license][license].

[blog]: https://buf.build/blog/connect-a-better-grpc
[buf-studio]: https://buf.build/studio
[connect-crosstest]: https://github.com/bufbuild/connect-crosstest
[connect-go]: https://github.com/bufbuild/connect-go
[connect-protocol]: https://connect.build/docs/protocol
[connect-swift]: https://github.com/bufbuild/connect-swift
[connect-web]: https://www.npmjs.com/package/@bufbuild/connect-web
[error-handling]: https://connect.build/docs/kotlin/errors
[getting-started]: https://connect.build/docs/kotlin/getting-started
[grpc-protocol]: https://github.com/grpc/grpc/blob/master/doc/PROTOCOL-HTTP2.md
[grpc-web-protocol]: https://github.com/grpc/grpc-web
[interceptors]: https://connect.build/docs/kotlin/interceptors
[license]: https://github.com/bufbuild/connect-go/blob/main/LICENSE
[protobuf]: https://developers.google.com/protocol-buffers
[protocol]: https://connect.build/docs/protocol
[server reflection]: https://github.com/bufbuild/connect-grpcreflect-go
[slack]: https://buf.build/links/slack
[streaming]: https://connect.build/docs/kotlin/using-clients#using-generated-clients

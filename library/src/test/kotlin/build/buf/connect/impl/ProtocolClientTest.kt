package build.buf.connect.impl

import build.buf.connect.Codec
import build.buf.connect.MethodSpec
import build.buf.connect.ProtocolClientConfig
import build.buf.connect.SerializationStrategy
import okio.Buffer
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class ProtocolClientTest {
    private val serializationStrategy: SerializationStrategy = mock { }
    private val codec: Codec<String> = mock { }

    @Test
    fun urlConfiguration() {
        whenever(codec.encodingName()).thenReturn("testing")
        whenever(codec.serialize(any())).thenReturn(Buffer())
        whenever(serializationStrategy.codec<String>(any())).thenReturn(codec)

        val client = ProtocolClient(
            httpClient = mock { },
            config = ProtocolClientConfig(
                host = "https://buf.build/",
                serializationStrategy = serializationStrategy
            )
        )
        client.unary(
            "input",
            emptyMap(),
            MethodSpec(
                path = "build.buf.connect.SomeService/Service",
                String::class,
                String::class
            )
        ) { _ -> }
        val client2 = ProtocolClient(
            httpClient = mock { },
            config = ProtocolClientConfig(
                host = "https://buf.build",
                serializationStrategy = serializationStrategy
            )
        )
        client2.unary(
            "input",
            emptyMap(),
            MethodSpec(
                path = "build.buf.connect.SomeService/Service",
                String::class,
                String::class
            )
        ) { _ -> }
    }
}

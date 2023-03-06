package build.buf.connect

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.Test
import org.mockito.kotlin.mock
import java.net.MalformedURLException


class ProtocolClientConfigTest {

    @Test
    fun hostUri() {
        val config = ProtocolClientConfig(
            host = "https://connect.build",
            serializationStrategy = mock { }
        )
        assertThat(config.baseUri.host).isEqualTo("connect.build")
        assertThat(config.baseUri.toURL()).isNotNull()
    }

    @Test(expected = MalformedURLException::class)
    fun unsupportedSchemeErrorsWhenTranslatingtoURL() {
        val config = ProtocolClientConfig(
            host = "xhtp://connect.build",
            serializationStrategy = mock { }
        )
        config.baseUri.toURL()
        fail<Unit>("expecting URL construction to fail")
    }
}

package org.zalando.riptide.autoconfigure;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.*;
import org.zalando.riptide.*;

import javax.net.ssl.*;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.*;
import static org.zalando.riptide.Bindings.*;
import static org.zalando.riptide.Navigators.*;
import static org.zalando.riptide.PassRoute.*;

@SpringBootTest(classes = DefaultTestConfiguration.class, webEnvironment = NONE)
final class KeystoreIntegrationTest {

    @Autowired
    @Qualifier("github")
    private Http http;

    @Test
    void shouldTrustExample() {
        http.get("https://example.com").dispatch(series(), anySeries().call(pass())).join();
    }

    @Test
    void shouldDistrustAnyoneElse() {
        final Exception exception = assertThrows(Exception.class,
                http.get("https://github.com").dispatch(series(), anySeries().call(pass()))::join);

        assertThat(exception.getCause(), is(instanceOf(SSLHandshakeException.class)));
        assertThat(exception.getMessage(),
                containsString("unable to find valid certification path to requested target"));
    }
}

package org.zalando.riptide.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.zalando.riptide.Http;

import javax.net.ssl.SSLHandshakeException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;
import static org.zalando.riptide.Bindings.anySeries;
import static org.zalando.riptide.Navigators.series;
import static org.zalando.riptide.PassRoute.pass;

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

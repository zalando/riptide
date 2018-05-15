package org.zalando.riptide.spring;

import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.zalando.riptide.Http;

import javax.net.ssl.SSLHandshakeException;

import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.zalando.riptide.Bindings.anySeries;
import static org.zalando.riptide.Navigators.series;
import static org.zalando.riptide.PassRoute.pass;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = DefaultTestConfiguration.class)
public class KeystoreIntegrationTest {

    @Autowired
    @Qualifier("github")
    private Http http;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void shouldTrustExample() {
        http.get("https://example.com").dispatch(series(), anySeries().call(pass())).join();
    }

    @Test
    public void shouldDistrustAnyoneElse() {
        expectedException.expectCause(instanceOf(SSLHandshakeException.class));
        expectedException.expectMessage(Matchers.containsString("unable to find valid certification path to requested target"));

        http.get("https://github.com").dispatch(series(), anySeries().call(pass())).join();
    }
}

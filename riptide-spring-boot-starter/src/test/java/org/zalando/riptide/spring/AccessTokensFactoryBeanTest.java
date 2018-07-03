package org.zalando.riptide.spring;

import org.junit.Test;
import org.zalando.riptide.spring.RiptideProperties.Client;
import org.zalando.riptide.spring.RiptideProperties.Client.OAuth;
import org.zalando.riptide.spring.RiptideProperties.Defaults;
import org.zalando.riptide.spring.RiptideProperties.GlobalOAuth;

import java.net.URI;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;

public final class AccessTokensFactoryBeanTest {

    private final AccessTokensFactoryBean unit = new AccessTokensFactoryBean(new RiptideProperties(
            new Defaults(),
            new GlobalOAuth(
                    URI.create("http://localhost"),
                    null,
                    TimeSpan.of(5, MINUTES),
                    TimeSpan.of(1, SECONDS),
                    TimeSpan.of(1, SECONDS)
            ),
            singletonMap(
                    "example", new Client(
                            null, null, null, null, null, null, null, null,
                            new OAuth(singletonList("example")),
                            null, null, null, null, null, null, null, false, null)
            )
    ));

    // just because spring sometimes fails to destroy properly during tests
    @Test
    public void shouldDestroy() throws Exception {
        unit.afterPropertiesSet();
        unit.getObject();
        unit.destroy();
    }

}

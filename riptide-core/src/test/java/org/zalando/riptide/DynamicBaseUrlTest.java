package org.zalando.riptide;

import java.net.URI;
import org.junit.After;
import org.junit.Test;
import org.springframework.test.web.client.MockRestServiceServer;

import static org.springframework.http.HttpStatus.Series.SUCCESSFUL;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.zalando.riptide.Bindings.on;
import static org.zalando.riptide.Navigators.series;
import static org.zalando.riptide.Route.pass;

public class DynamicBaseUrlTest {

    private final Rest unit;
    private final MockRestServiceServer server;
    private URI baseUrl;

    public DynamicBaseUrlTest() {
        final MockSetup setup = new MockSetup();

        this.unit = setup.getRestBuilder().baseUrlProvider(this::getBaseUrl).build();
        this.server = setup.getServer();
    }

    private URI getBaseUrl() {
        return baseUrl;
    }

    @After
    public void after() {
        server.verify();
    }

    @Test
    public void shouldUseDynamicBaseUrl() {
        expectRequestTo("https://host1.example.com/123");
        expectRequestTo("https://host2.example.com/123");

        baseUrl = URI.create("https://host1.example.com");
        unit.get("/123")
                .dispatch(series(),
                        on(SUCCESSFUL).call(pass()));

        baseUrl = URI.create("https://host2.example.com");
        unit.get("/123")
                .dispatch(series(),
                        on(SUCCESSFUL).call(pass()));
    }

    private void expectRequestTo(final String url) {
        server.expect(requestTo(url))
                .andRespond(withSuccess());
    }

}

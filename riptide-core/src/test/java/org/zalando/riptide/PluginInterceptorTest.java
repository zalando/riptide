package org.zalando.riptide;

import org.junit.Before;
import org.junit.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.net.NoRouteToHostException;

import static java.util.Collections.singletonList;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

public final class PluginInterceptorTest {

    private final RestTemplate template = new RestTemplate();
    private final MockRestServiceServer server = MockRestServiceServer.createServer(template);

    @Before
    public void configureInterceptors() {
        template.setInterceptors(singletonList(
                new PluginInterceptor(new NoRouteToHostPlugin())));
    }

    @Test
    public void shouldSucceedToPerformRequest() {
        server.expect(requestTo("http://unknown/foo")).andRespond(withSuccess());

        template.getForEntity("http://unknown/foo", Object.class);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void shouldFailToPerformRequest() {
        server.expect(requestTo("http://unknown/foo")).andRespond(request -> {
            throw new NoRouteToHostException();
        });

        template.getForEntity("http://unknown/foo", Object.class);
    }

}

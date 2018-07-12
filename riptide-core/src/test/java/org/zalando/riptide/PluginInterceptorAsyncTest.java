package org.zalando.riptide;

import org.junit.Before;
import org.junit.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.AsyncRestTemplate;

import java.net.NoRouteToHostException;
import java.util.concurrent.ExecutionException;

import static java.util.Collections.singletonList;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

public final class PluginInterceptorAsyncTest {

    private final AsyncRestTemplate template = new AsyncRestTemplate();
    private final MockRestServiceServer server = MockRestServiceServer.createServer(template);

    @Before
    public void configureInterceptors() {
        template.setInterceptors(singletonList(
                new PluginInterceptor(new NoRouteToHostPlugin())));
    }

    @Test
    public void shouldSucceedToPerformRequest() throws Exception {
        server.expect(requestTo("http://unknown/foo")).andRespond(withSuccess());

        template.getForEntity("http://unknown/foo", Object.class).get();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void shouldFailToPerformRequest() throws Throwable {
        server.expect(requestTo("http://unknown/foo")).andRespond(request -> {
            throw new NoRouteToHostException();
        });

        try {
            template.getForEntity("http://unknown/foo", Object.class).get();
        } catch (final ExecutionException e) {
            throw e.getCause();
        }
    }

}

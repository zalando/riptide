package org.zalando.riptide;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.AsyncRestTemplate;

import java.net.NoRouteToHostException;
import java.util.concurrent.ExecutionException;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

final class PluginInterceptorAsyncTest {

    private final AsyncRestTemplate template = new AsyncRestTemplate();
    private final MockRestServiceServer server = MockRestServiceServer.createServer(template);

    @BeforeEach
    void configureInterceptors() {
        template.setInterceptors(singletonList(
                new PluginInterceptor(new NoRouteToHostPlugin())));
    }

    @Test
    void shouldSucceedToPerformRequest() throws Exception {
        server.expect(requestTo("http://unknown/foo")).andRespond(withSuccess());

        template.getForEntity("http://unknown/foo", Object.class).get();
    }

    @Test
    void shouldFailToPerformRequest() {
        server.expect(requestTo("http://unknown/foo")).andRespond(request -> {
            throw new NoRouteToHostException();
        });

        final ExecutionException exception = assertThrows(ExecutionException.class, () ->
                template.getForEntity("http://unknown/foo", Object.class).get());

        assertThat(exception.getCause(), is(instanceOf(UnsupportedOperationException.class)));
    }

}

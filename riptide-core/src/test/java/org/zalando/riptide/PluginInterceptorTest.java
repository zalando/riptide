package org.zalando.riptide;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.net.NoRouteToHostException;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

final class PluginInterceptorTest {

    private final RestTemplate template = new RestTemplate();
    private final MockRestServiceServer server = MockRestServiceServer.createServer(template);

    @BeforeEach
    void configureInterceptors() {
        template.setInterceptors(singletonList(
                new PluginInterceptor(new NoRouteToHostPlugin())));
    }

    @Test
    void shouldSucceedToPerformRequest() {
        server.expect(requestTo("http://unknown/foo")).andRespond(withSuccess());

        template.getForEntity("http://unknown/foo", Object.class);
    }

    @Test
    void shouldFailToPerformRequest() {
        server.expect(requestTo("http://unknown/foo")).andRespond(request -> {
            throw new NoRouteToHostException();
        });


        assertThrows(UnsupportedOperationException.class, () ->
                template.getForEntity("http://unknown/foo", Object.class));
    }

}

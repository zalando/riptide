package org.zalando.riptide.autoconfigure.testing;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.client.MockRestServiceServer;
import org.zalando.riptide.autoconfigure.RiptideClientTest;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@RiptideClientTest
@ActiveProfiles("testing")
final class RiptideClientTestTest {

    @Configuration
    @Import(TestService.class)
    static class ContextConfiguration {

    }

    @Autowired
    private TestService client;

    @Autowired
    private MockRestServiceServer server;

    @Test
    void shouldAutowireMockedHttp() {
        server.expect(requestTo("https://example.com/foo/bar")).andRespond(withSuccess());
        client.callViaHttp();
        server.verify();
    }

}

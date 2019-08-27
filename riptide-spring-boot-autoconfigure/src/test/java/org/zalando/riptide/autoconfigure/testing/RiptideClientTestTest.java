package org.zalando.riptide.autoconfigure.testing;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.context.annotation.*;
import org.springframework.test.context.*;
import org.springframework.test.web.client.*;
import org.zalando.riptide.autoconfigure.*;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

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

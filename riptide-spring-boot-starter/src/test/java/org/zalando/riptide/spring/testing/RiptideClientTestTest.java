package org.zalando.riptide.spring.testing;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.client.MockRestServiceServer;
import org.zalando.riptide.spring.RiptideClientTest;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@RunWith(SpringRunner.class)
@SpringBootTest // needed by @RestClientTest - that's why we are in our own package here
@RiptideClientTest
@ActiveProfiles("testing")
public class RiptideClientTestTest {

    @Configuration
    @Import(TestService.class)
    static class ContextConfiguration {

    }

    @Autowired
    private TestService client;

    @Autowired
    private MockRestServiceServer server;

    @Test
    public void shouldAutowireMockedHttp() {
        server.expect(requestTo("https://example.com/foo/bar")).andRespond(withSuccess());
        client.callViaHttp();
        server.verify();
    }

}

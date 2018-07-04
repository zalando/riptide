package org.zalando.riptide.spring.url;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.client.MockRestServiceServer;
import org.zalando.logbook.spring.LogbookAutoConfiguration;
import org.zalando.riptide.Http;
import org.zalando.riptide.spring.MetricsTestAutoConfiguration;
import org.zalando.riptide.spring.RiptideClientTest;
import org.zalando.tracer.spring.TracerAutoConfiguration;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.zalando.riptide.PassRoute.pass;

@RunWith(SpringRunner.class)
@SpringBootTest
@RiptideClientTest
@ActiveProfiles("default")
public class UrlResolutionTest {

    @Configuration
    @ImportAutoConfiguration({
            JacksonAutoConfiguration.class,
            LogbookAutoConfiguration.class,
            TracerAutoConfiguration.class,
            MetricsTestAutoConfiguration.class,
    })
    @ActiveProfiles("default")
    static class ContextConfiguration {

    }

    @Autowired
    private MockRestServiceServer server;

    @Autowired
    @Qualifier("example")
    private Http unit;

    @Test
    public void shouldAppendUrl() {
        server.expect(requestTo("https://example.com/foo/bar"))
                .andRespond(withSuccess());

        unit.get("/bar")
                .call(pass())
                .join();

        server.verify();
    }

}

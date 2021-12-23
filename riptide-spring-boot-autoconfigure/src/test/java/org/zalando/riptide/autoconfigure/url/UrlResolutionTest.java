package org.zalando.riptide.autoconfigure.url;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.client.MockRestServiceServer;
import org.zalando.logbook.autoconfigure.LogbookAutoConfiguration;
import org.zalando.opentracing.flowid.autoconfigure.OpenTracingFlowIdAutoConfiguration;
import org.zalando.riptide.Http;
import org.zalando.riptide.autoconfigure.MetricsTestAutoConfiguration;
import org.zalando.riptide.autoconfigure.OpenTracingTestAutoConfiguration;
import org.zalando.riptide.autoconfigure.RiptideClientTest;
import org.zalando.riptide.opentracing.span.HttpUrlSpanDecorator;
import org.zalando.riptide.opentracing.span.SpanDecorator;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.zalando.riptide.PassRoute.pass;

@RiptideClientTest
@ActiveProfiles("default")
final class UrlResolutionTest {

    @Configuration
    @ImportAutoConfiguration({
            JacksonAutoConfiguration.class,
            LogbookAutoConfiguration.class,
            OpenTracingFlowIdAutoConfiguration.class,
            OpenTracingTestAutoConfiguration.class,
            MetricsTestAutoConfiguration.class,
    })
    @ActiveProfiles("default")
    static class ContextConfiguration {

        @Bean
        public SpanDecorator exampleSpanDecorator() {
            return new HttpUrlSpanDecorator();
        }
    }

    @Autowired
    private MockRestServiceServer server;

    @Autowired
    @Qualifier("example")
    private Http unit;

    @Test
    void shouldAppendUrl() {
        server.expect(requestTo("https://example.com/foo/bar"))
              .andRespond(withSuccess());

        unit.get("/bar")
            .call(pass())
            .join();

        server.verify();
    }
}

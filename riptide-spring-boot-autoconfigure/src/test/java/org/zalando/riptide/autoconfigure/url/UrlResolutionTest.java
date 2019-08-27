package org.zalando.riptide.autoconfigure.url;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.boot.autoconfigure.jackson.*;
import org.springframework.context.annotation.*;
import org.springframework.test.context.*;
import org.springframework.test.web.client.*;
import org.zalando.logbook.autoconfigure.*;
import org.zalando.riptide.*;
import org.zalando.riptide.autoconfigure.*;
import org.zalando.riptide.opentracing.span.*;
import org.zalando.tracer.autoconfigure.*;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import static org.zalando.riptide.PassRoute.*;

@RiptideClientTest
@ActiveProfiles("default")
final class UrlResolutionTest {

    @Configuration
    @ImportAutoConfiguration({
            JacksonAutoConfiguration.class,
            LogbookAutoConfiguration.class,
            TracerAutoConfiguration.class,
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

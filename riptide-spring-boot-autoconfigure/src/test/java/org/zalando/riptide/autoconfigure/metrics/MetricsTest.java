package org.zalando.riptide.autoconfigure.metrics;

import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.client.MockRestServiceServer;
import org.zalando.logbook.autoconfigure.LogbookAutoConfiguration;
import org.zalando.riptide.Http;
import org.zalando.riptide.autoconfigure.MetricsTestAutoConfiguration;
import org.zalando.riptide.autoconfigure.OpenTracingTestAutoConfiguration;
import org.zalando.riptide.autoconfigure.RiptideClientTest;
import org.zalando.tracer.autoconfigure.TracerAutoConfiguration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.zalando.riptide.Bindings.anyStatus;
import static org.zalando.riptide.Bindings.on;
import static org.zalando.riptide.Navigators.status;
import static org.zalando.riptide.PassRoute.pass;
import static org.zalando.riptide.failsafe.RetryRoute.retry;
import static org.zalando.riptide.micrometer.MicrometerPlugin.TAGS;

@RiptideClientTest
@ActiveProfiles("default")
final class MetricsTest {

    @Configuration
    @ImportAutoConfiguration({
            JacksonAutoConfiguration.class,
            LogbookAutoConfiguration.class,
            TracerAutoConfiguration.class,
            OpenTracingTestAutoConfiguration.class,
            MetricsTestAutoConfiguration.class,
    })
    static class ContextConfiguration {

    }

    @Autowired
    @Qualifier("foo")
    private Http foo;

    @Autowired
    @Qualifier("bar")
    private Http bar;

    @Autowired
    private MockRestServiceServer server;

    @Autowired
    private SimpleMeterRegistry registry;

    @Test
    void shouldRecordRequests() {
        server.expect(requestTo("http://foo")).andRespond(withSuccess());
        server.expect(requestTo("http://bar")).andRespond(withSuccess());

        foo.get().call(pass()).join();
        bar.get().call(pass()).join();

        final List<Timer> timers = timers("http.client.requests");

        assertEquals(2, timers.size());

        {
            final Timer timer = timers.get(0);
            assertEquals("GET", timer.getId().getTag("http.method"));
            assertEquals("foo", timer.getId().getTag("client_id"));
        }

        {
            final Timer timer = timers.get(1);
            assertEquals("GET", timer.getId().getTag("http.method"));
            assertEquals("bar", timer.getId().getTag("client_id"));
        }
    }

    @Test
    void shouldRecordRetries() {
        server.expect(requestTo("http://foo")).andRespond(withStatus(SERVICE_UNAVAILABLE));
        server.expect(requestTo("http://foo")).andRespond(withStatus(SERVICE_UNAVAILABLE));
        server.expect(requestTo("http://foo")).andRespond(withSuccess());

        foo.get()
                .attribute(TAGS, Tags.of("test", "retries"))
                .dispatch(status(),
                        on(SERVICE_UNAVAILABLE).call(retry()),
                        anyStatus().call(pass()))
                .join();

        final List<Timer> timers = timers("http.client.requests",
                Tag.of("test", "retries"));

        assertEquals(3, timers.size());

        {
            final Timer timer = timers.get(0);
            assertNull(timer.getId().getTag("retry_number"));
            assertEquals("foo", timer.getId().getTag("client_id"));
        }

        {
            final Timer timer = timers.get(1);
            assertEquals("1", timer.getId().getTag("retry_number"));
            assertEquals("foo", timer.getId().getTag("client_id"));
        }

        {
            final Timer timer = timers.get(2);
            assertEquals("2", timer.getId().getTag("retry_number"));
            assertEquals("foo", timer.getId().getTag("client_id"));
        }
    }

    @Test
    void shouldRecordCircuitBreakers() {
        server.expect(requestTo("http://bar")).andRespond(withStatus(SERVICE_UNAVAILABLE));
        server.expect(requestTo("http://bar")).andRespond(withStatus(SERVICE_UNAVAILABLE));
        server.expect(requestTo("http://bar")).andRespond(withStatus(SERVICE_UNAVAILABLE));
        server.expect(requestTo("http://bar")).andRespond(withStatus(SERVICE_UNAVAILABLE));
        server.expect(requestTo("http://bar")).andRespond(withSuccess());

        bar.get()
                .dispatch(status(),
                        on(SERVICE_UNAVAILABLE).call(retry()),
                        anyStatus().call(pass()))
                .join();

        final List<Timer> timers = timers("http.client.circuit-breakers");

        assertEquals(2, timers.size());

        final Timer halfOpen = timers.get(0);
        assertEquals("HALF_OPEN", halfOpen.getId().getTag("state"));
        assertEquals("bar", halfOpen.getId().getTag("client_id"));
        assertEquals(4, halfOpen.count());

        final Timer open = timers.get(1);
        assertEquals("OPEN", open.getId().getTag("state"));
        assertEquals("bar", open.getId().getTag("client_id"));
        assertEquals(4, open.count());
    }

    private List<Timer> timers(final String name, final Tag... tags) {
        final List<Timer> timers = new ArrayList<>(registry.find(name)
                .tags(Arrays.asList(tags)).timers());
        Collections.reverse(timers);
        return timers;
    }

}

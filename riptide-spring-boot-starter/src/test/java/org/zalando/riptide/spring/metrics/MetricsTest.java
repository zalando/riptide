package org.zalando.riptide.spring.metrics;

import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import net.jodah.failsafe.CircuitBreakerOpenException;
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
import org.zalando.riptide.capture.Completion;
import org.zalando.riptide.spring.MetricsTestAutoConfiguration;
import org.zalando.riptide.spring.RiptideClientTest;
import org.zalando.tracer.spring.TracerAutoConfiguration;

import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.collect.Ordering.from;
import static java.util.Comparator.comparing;
import static org.junit.Assert.assertEquals;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.zalando.riptide.Bindings.anyStatus;
import static org.zalando.riptide.Bindings.on;
import static org.zalando.riptide.Navigators.status;
import static org.zalando.riptide.PassRoute.pass;
import static org.zalando.riptide.failsafe.RetryRoute.retry;

@RunWith(SpringRunner.class)
@SpringBootTest
@RiptideClientTest
@ActiveProfiles("default")
public class MetricsTest {

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
    public void shouldRecordRetries() {
        server.expect(requestTo("http://foo")).andRespond(withStatus(SERVICE_UNAVAILABLE));
        server.expect(requestTo("http://foo")).andRespond(withStatus(SERVICE_UNAVAILABLE));
        server.expect(requestTo("http://foo")).andRespond(withSuccess());

        foo.get()
                .dispatch(status(),
                        on(SERVICE_UNAVAILABLE).call(retry()),
                        anyStatus().call(pass()))
                .join();

        final List<Timer> timers = timers("http.client.retries");

        assertEquals(2, timers.size());
        assertEquals("1", timers.get(0).getId().getTag("retries"));
        assertEquals("2", timers.get(1).getId().getTag("retries"));
    }

    @Test
    public void shouldRecordCircuitBreakers() throws InterruptedException {
        server.expect(requestTo("http://bar")).andRespond(withStatus(SERVICE_UNAVAILABLE));
        server.expect(requestTo("http://bar")).andRespond(withStatus(SERVICE_UNAVAILABLE));
        server.expect(requestTo("http://bar")).andRespond(withStatus(SERVICE_UNAVAILABLE));
        server.expect(requestTo("http://bar")).andRespond(withSuccess());
        server.expect(requestTo("http://bar")).andRespond(withSuccess());

        for (int i = 0; i < 5; i++) {
            Thread.sleep(10);

            try {
                Completion.join(bar.get().call(pass()));
            } catch (final CircuitBreakerOpenException e) {
                // expected
            }
        }

        final List<Timer> timers = timers("http.client.circuit-breakers");

        assertEquals(2, timers.size());

        final Timer halfOpen = timers.get(0);
        assertEquals("HALF_OPEN", halfOpen.getId().getTag("state"));
        assertEquals(2, halfOpen.count());

        final Timer open = timers.get(1);
        assertEquals("OPEN", open.getId().getTag("state"));
        assertEquals(2, open.count());
    }

    private List<Timer> timers(final String name) {
        return registry.find(name).timers().stream()
                .sorted(comparing(this::tags, from(comparing(Tag::getKey)
                        .thenComparing(Tag::getValue)).lexicographical()))
                .collect(Collectors.toList());
    }

    private List<Tag> tags(final Timer timer) {
        return timer.getId().getTags();
    }

}

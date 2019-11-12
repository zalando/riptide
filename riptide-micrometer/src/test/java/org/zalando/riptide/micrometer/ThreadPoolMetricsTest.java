package org.zalando.riptide.micrometer;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;

import static java.util.concurrent.TimeUnit.MINUTES;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.zalando.fauxpas.FauxPas.throwingRunnable;

final class ThreadPoolMetricsTest {

    private final ThreadPoolExecutor executor =
            new ThreadPoolExecutor(1, 3, 1, MINUTES, new LinkedBlockingQueue<>(1));

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();

    @BeforeEach
    void setUp() {
        new ThreadPoolMetrics(executor)
                .withMetricName("http.client.threads")
                .withDefaultTags(Tag.of("application", "test"))
                .bindTo(registry);
    }

    @Test
    void shouldMeasureInitial() {
        assertThat(gauge("http.client.threads.available").value(), is(0.0));
        assertThat(gauge("http.client.threads.leased").value(), is(0.0));
        assertThat(gauge("http.client.threads.total").value(), is(0.0));
        assertThat(gauge("http.client.threads.min").value(), is(1.0));
        assertThat(gauge("http.client.threads.max").value(), is(3.0));
        assertThat(gauge("http.client.threads.queued").value(), is(0.0));
    }

    @Test
    void shouldMeasureIdle() throws InterruptedException {
        executor.execute(() -> {});

        Thread.sleep(1000);

        assertThat(gauge("http.client.threads.available").value(), is(1.0));
        assertThat(gauge("http.client.threads.leased").value(), is(0.0));
        assertThat(gauge("http.client.threads.total").value(), is(1.0));
        assertThat(gauge("http.client.threads.min").value(), is(1.0));
        assertThat(gauge("http.client.threads.max").value(), is(3.0));
        assertThat(gauge("http.client.threads.queued").value(), is(0.0));
    }

    @Test
    void shouldMeasureFull() {
        executor.execute(throwingRunnable(() -> Thread.sleep(1000)));
        executor.execute(throwingRunnable(() -> Thread.sleep(1000)));
        executor.execute(throwingRunnable(() -> Thread.sleep(1000)));
        executor.execute(throwingRunnable(() -> Thread.sleep(1000)));

        assertThat(gauge("http.client.threads.available").value(), is(0.0));
        assertThat(gauge("http.client.threads.leased").value(), is(3.0));
        assertThat(gauge("http.client.threads.total").value(), is(3.0));
        assertThat(gauge("http.client.threads.min").value(), is(1.0));
        assertThat(gauge("http.client.threads.max").value(), is(3.0));
        assertThat(gauge("http.client.threads.queued").value(), is(1.0));
    }

    private Gauge gauge(final String name) {
        return registry.find(name).tag("application", "test").gauge();
    }

}

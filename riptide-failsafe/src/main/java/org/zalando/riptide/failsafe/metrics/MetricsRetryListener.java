package org.zalando.riptide.failsafe.metrics;

import com.google.common.collect.ImmutableList;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import net.jodah.failsafe.event.ExecutionAttemptedEvent;
import org.apiguardian.api.API;
import org.springframework.http.client.ClientHttpResponse;
import org.zalando.riptide.RequestArguments;
import org.zalando.riptide.failsafe.RetryListener;
import org.zalando.riptide.metrics.DefaultTagGenerator;
import org.zalando.riptide.metrics.TagGenerator;

import javax.annotation.Nullable;
import java.time.Duration;

import static com.google.common.collect.Iterables.concat;
import static java.util.Collections.singleton;
import static org.apiguardian.api.API.Status.EXPERIMENTAL;

@API(status = EXPERIMENTAL)
public final class MetricsRetryListener implements RetryListener {

    private final MeterRegistry registry;
    private final String metricName;
    private final ImmutableList<Tag> defaultTags;
    private final TagGenerator generator = new DefaultTagGenerator();

    public MetricsRetryListener(final MeterRegistry registry) {
        this(registry, "http.client.retries", ImmutableList.of());
    }

    private MetricsRetryListener(final MeterRegistry registry, final String metricName,
            final ImmutableList<Tag> defaultTags) {

        this.registry = registry;
        this.metricName = metricName;
        this.defaultTags = defaultTags;
    }

    public MetricsRetryListener withMetricName(final String metricName) {
        return new MetricsRetryListener(registry, metricName, defaultTags);
    }

    public MetricsRetryListener withDefaultTags(final Tag... defaultTags) {
        return withDefaultTags(ImmutableList.copyOf(defaultTags));
    }

    public MetricsRetryListener withDefaultTags(final Iterable<Tag> defaultTags) {
        return new MetricsRetryListener(registry, metricName, ImmutableList.copyOf(defaultTags));
    }

    @Override
    public void onRetry(final RequestArguments arguments,
            final ExecutionAttemptedEvent<ClientHttpResponse> event) {

        final Iterable<Tag> tags = tags(arguments, event);
        registry.timer(metricName, tags).record(Duration.ofNanos(event.getElapsedTime().toNanos()));
    }

    private Iterable<Tag> tags(final RequestArguments arguments,
            final ExecutionAttemptedEvent<ClientHttpResponse> event) {
        return concat(tags(arguments, event.getLastResult(), event.getLastFailure()), tags(event));
    }

    private Iterable<Tag> tags(final RequestArguments arguments, @Nullable final ClientHttpResponse response,
            @Nullable final Throwable failure) {
        return concat(defaultTags, generator.tags(arguments, response, failure));
    }

    private Iterable<Tag> tags(final ExecutionAttemptedEvent<ClientHttpResponse> event) {
        return singleton(Tag.of("retries", String.valueOf(event.getAttemptCount())));
    }

}

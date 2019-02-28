package org.zalando.riptide.metrics;

import com.google.common.collect.ImmutableList;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Timer.Sample;
import lombok.AllArgsConstructor;
import org.apiguardian.api.API;
import org.springframework.http.client.ClientHttpResponse;
import org.zalando.riptide.Plugin;
import org.zalando.riptide.RequestArguments;
import org.zalando.riptide.RequestExecution;

import static com.google.common.collect.Iterables.concat;
import static org.apiguardian.api.API.Status.EXPERIMENTAL;

@API(status = EXPERIMENTAL)
public final class MetricsPlugin implements Plugin {

    private final MeterRegistry registry;
    private final String metricName;
    private final ImmutableList<Tag> defaultTags;
    private final TagGenerator generator = new DefaultTagGenerator();

    public MetricsPlugin(final MeterRegistry registry) {
        this(registry, "http.client.requests", ImmutableList.of());
    }

    private MetricsPlugin(final MeterRegistry registry, final String metricName, final ImmutableList<Tag> defaultTags) {
        this.registry = registry;
        this.metricName = metricName;
        this.defaultTags = defaultTags;
    }

    public MetricsPlugin withMetricName(final String metricName) {
        return new MetricsPlugin(registry, metricName, defaultTags);
    }

    public MetricsPlugin withDefaultTags(final Tag... defaultTags) {
        return withDefaultTags(ImmutableList.copyOf(defaultTags));
    }

    public MetricsPlugin withDefaultTags(final Iterable<Tag> defaultTags) {
        return new MetricsPlugin(registry, metricName, ImmutableList.copyOf(defaultTags));
    }

    @Override
    public RequestExecution aroundNetwork(final RequestExecution execution) {
        return arguments -> {
            final Measurement measurement = new Measurement(arguments);

            return execution.execute(arguments)
                    .whenComplete(measurement::record);
        };
    }

    @AllArgsConstructor
    private final class Measurement {

        private final Sample sample = Timer.start(registry);
        private final RequestArguments arguments;

        void record(final ClientHttpResponse response, final Throwable throwable) {
            final Iterable<Tag> tags = concat(defaultTags, generator.tags(arguments, response, throwable));
            final Timer timer = registry.timer(metricName, tags);
            sample.stop(timer);
        }

    }

}

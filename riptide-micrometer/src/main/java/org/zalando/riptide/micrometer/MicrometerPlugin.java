package org.zalando.riptide.micrometer;

import com.google.common.collect.*;
import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.Timer.*;
import lombok.*;
import org.apiguardian.api.*;
import org.springframework.http.client.*;
import org.zalando.riptide.*;

import static com.google.common.collect.Iterables.*;
import static org.apiguardian.api.API.Status.*;

@API(status = EXPERIMENTAL)
public final class MicrometerPlugin implements Plugin {

    private final MeterRegistry registry;
    private final String metricName;
    private final ImmutableList<Tag> defaultTags;
    private final TagGenerator generator = new DefaultTagGenerator();

    public MicrometerPlugin(final MeterRegistry registry) {
        this(registry, "http.client.requests", ImmutableList.of());
    }

    private MicrometerPlugin(final MeterRegistry registry, final String metricName, final ImmutableList<Tag> defaultTags) {
        this.registry = registry;
        this.metricName = metricName;
        this.defaultTags = defaultTags;
    }

    public MicrometerPlugin withMetricName(final String metricName) {
        return new MicrometerPlugin(registry, metricName, defaultTags);
    }

    public MicrometerPlugin withDefaultTags(final Tag... defaultTags) {
        return withDefaultTags(ImmutableList.copyOf(defaultTags));
    }

    public MicrometerPlugin withDefaultTags(final Iterable<Tag> defaultTags) {
        return new MicrometerPlugin(registry, metricName, ImmutableList.copyOf(defaultTags));
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

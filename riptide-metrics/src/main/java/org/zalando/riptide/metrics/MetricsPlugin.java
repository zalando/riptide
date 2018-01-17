package org.zalando.riptide.metrics;

import com.google.common.base.Stopwatch;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.AllArgsConstructor;
import org.springframework.http.client.ClientHttpResponse;
import org.zalando.riptide.Plugin;
import org.zalando.riptide.RequestArguments;
import org.zalando.riptide.RequestExecution;

import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.zalando.fauxpas.FauxPas.throwingBiConsumer;

public final class MetricsPlugin implements Plugin {

    private final MeterRegistry registry;
    private final MetricsNameGenerator generator;

    public MetricsPlugin(final MeterRegistry registry, final MetricsNameGenerator generator) {
        this.registry = registry;
        this.generator = generator;
    }

    @Override
    public RequestExecution beforeSend(final RequestArguments originalArguments, final RequestExecution execution) {
        return arguments -> {
            final Measurement measurement = new Measurement(arguments);

            // stop measurement early, ...
            final CompletableFuture<ClientHttpResponse> future = execution.execute(arguments)
                    .whenComplete(throwingBiConsumer(measurement::stop));

            // ... but delay actual recording
            future.whenComplete(throwingBiConsumer(measurement::record));

            return future;
        };
    }

    @AllArgsConstructor
    private final class Measurement {
        private final Stopwatch stopwatch = Stopwatch.createStarted();
        private final RequestArguments arguments;

        @SuppressWarnings("unused")
        void stop(@Nullable final ClientHttpResponse response,
                @Nullable final Throwable e) {
            stopwatch.stop();
        }

        void record(@Nullable final ClientHttpResponse response,
                @SuppressWarnings("unused") @Nullable final Throwable e) throws Exception {

            if (response == null) {
                return;
            }

            final long elapsed = stopwatch.elapsed(TimeUnit.MILLISECONDS);
            final String metricName = generator.generate(arguments, response);

            registry.gauge(metricName, elapsed);
        }
    }

}

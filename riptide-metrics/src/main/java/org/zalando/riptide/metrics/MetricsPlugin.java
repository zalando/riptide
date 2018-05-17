package org.zalando.riptide.metrics;

import com.google.common.base.Stopwatch;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.AllArgsConstructor;
import org.apiguardian.api.API;
import org.springframework.http.client.ClientHttpResponse;
import org.zalando.riptide.Plugin;
import org.zalando.riptide.RequestArguments;
import org.zalando.riptide.RequestExecution;

import static java.util.Objects.nonNull;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.apiguardian.api.API.Status.EXPERIMENTAL;
import static org.zalando.fauxpas.FauxPas.throwingBiConsumer;

@API(status = EXPERIMENTAL)
public final class MetricsPlugin implements Plugin {

    private final MeterRegistry registry;
    private final MetricsNameGenerator generator;

    public MetricsPlugin(final MeterRegistry registry, final MetricsNameGenerator generator) {
        this.registry = registry;
        this.generator = generator;
    }

    @Override
    public RequestExecution interceptBeforeRouting(final RequestArguments arguments, final RequestExecution execution) {
        return () -> {
            final Measurement measurement = new Measurement(arguments);

            return execution.execute()
                    .whenComplete(throwingBiConsumer((response, e) -> {
                        if (nonNull(response)) {
                            measurement.record(response);
                        }
                    }));
        };
    }

    @Override
    public RequestExecution prepare(final RequestArguments arguments, final RequestExecution execution) {
        return execution;
    }

    @AllArgsConstructor
    private final class Measurement {

        private final Stopwatch stopwatch = Stopwatch.createStarted();
        private final RequestArguments arguments;

        void record(final ClientHttpResponse response) throws Exception {
            stopwatch.stop();

            final Timer timer = getTimer(response);
            final long duration = stopwatch.elapsed(MILLISECONDS);

            timer.record(duration, MILLISECONDS);
        }

        private Timer getTimer(final ClientHttpResponse response) throws Exception {
            final String metricName = generator.generate(arguments, response);
            return registry.timer(metricName);
        }

    }

}

package org.zalando.riptide.metrics;

import com.google.common.base.Stopwatch;
import lombok.AllArgsConstructor;
import org.springframework.boot.actuate.metrics.GaugeService;
import org.springframework.http.client.ClientHttpResponse;
import org.zalando.riptide.Plugin;
import org.zalando.riptide.RequestArguments;
import org.zalando.riptide.RequestExecution;

import javax.annotation.Nullable;
import java.util.concurrent.TimeUnit;

import static org.zalando.fauxpas.FauxPas.throwingBiConsumer;

public final class MetricsPlugin implements Plugin {

    private final GaugeService gaugeService;
    private final MetricsNameGenerator generator;

    public MetricsPlugin(final GaugeService gaugeService, final MetricsNameGenerator generator) {
        this.gaugeService = gaugeService;
        this.generator = generator;
    }

    @Override
    public RequestExecution interceptBeforeRouting(final RequestArguments arguments, final RequestExecution execution) {
        final Metric metric = new Metric(arguments);

        return () -> execution.execute()
                .whenComplete(throwingBiConsumer(metric::record));
    }

    @Override
    public RequestExecution prepare(final RequestArguments arguments, final RequestExecution execution) {
        return execution;
    }

    @AllArgsConstructor
    private final class Metric {
        private final Stopwatch stopwatch = Stopwatch.createStarted();
        private final RequestArguments arguments;

        void record(@Nullable final ClientHttpResponse response, @Nullable final Throwable e) throws Exception {
            stopwatch.stop();

            if (response == null) {
                return;
            }

            final long elapsed = stopwatch.elapsed(TimeUnit.MILLISECONDS);
            final String metricName = generator.generate(arguments, response);
            gaugeService.submit(metricName, elapsed);
        }
    }

}

package org.zalando.riptide.metrics;

import com.google.common.base.Stopwatch;
import lombok.AllArgsConstructor;
import org.apiguardian.api.API;
import org.springframework.boot.actuate.metrics.GaugeService;
import org.springframework.http.client.ClientHttpResponse;
import org.zalando.riptide.Plugin;
import org.zalando.riptide.RequestArguments;
import org.zalando.riptide.RequestExecution;

import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;
import static org.zalando.fauxpas.FauxPas.throwingBiConsumer;

@API(status = EXPERIMENTAL)
public final class MetricsPlugin implements Plugin {

    private final GaugeService gaugeService;
    private final MetricsNameGenerator generator;

    public MetricsPlugin(final GaugeService gaugeService, final MetricsNameGenerator generator) {
        this.gaugeService = gaugeService;
        this.generator = generator;
    }

    @Override
    public RequestExecution interceptBeforeRouting(final RequestArguments arguments, final RequestExecution execution) {
        return () -> {
            final Measurement measurement = new Measurement(arguments);

            // stop measurement early, ...
            final CompletableFuture<ClientHttpResponse> future = execution.execute()
                    .whenComplete(throwingBiConsumer(measurement::stop));

            // ... but delay actual recording
            future.whenComplete(throwingBiConsumer(measurement::record));

            return future;
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
            gaugeService.submit(metricName, elapsed);
        }
    }

}

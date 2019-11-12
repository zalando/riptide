package org.zalando.riptide.micrometer;

import com.google.common.collect.ImmutableList;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.MeterBinder;
import lombok.AllArgsConstructor;
import org.apiguardian.api.API;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Supplier;

import static com.google.common.collect.ImmutableList.copyOf;
import static io.micrometer.core.instrument.binder.BaseUnits.TASKS;
import static io.micrometer.core.instrument.binder.BaseUnits.THREADS;
import static lombok.AccessLevel.PRIVATE;
import static org.apiguardian.api.API.Status.EXPERIMENTAL;

@API(status = EXPERIMENTAL)
@AllArgsConstructor(access = PRIVATE)
public final class ThreadPoolMetrics implements MeterBinder {

    private final ThreadPoolExecutor executor;
    private final String metricName;
    private final ImmutableList<Tag> defaultTags;

    public ThreadPoolMetrics(final ThreadPoolExecutor executor) {
        this(executor, "http.client.threads", ImmutableList.of());
    }

    public ThreadPoolMetrics withMetricName(final String metricName) {
        return new ThreadPoolMetrics(executor, metricName, defaultTags);
    }

    public ThreadPoolMetrics withDefaultTags(final Tag... defaultTags) {
        return withDefaultTags(copyOf(defaultTags));
    }

    public ThreadPoolMetrics withDefaultTags(final Iterable<Tag> defaultTags) {
        return new ThreadPoolMetrics(executor, metricName, copyOf(defaultTags));
    }

    @Override
    public void bindTo(final MeterRegistry registry) {
        gauge("available", () -> executor.getPoolSize() - executor.getActiveCount())
                .description("The number idle threads")
                .baseUnit(THREADS)
                .register(registry);

        gauge("leased", executor::getActiveCount)
                .description("The number of threads that are actively executing tasks")
                .baseUnit(THREADS)
                .register(registry);

        gauge("total", executor::getPoolSize)
                .description("The number of threads that are currently in the pool")
                .baseUnit(THREADS)
                .register(registry);

        gauge("min", executor::getCorePoolSize)
                .description("The minimum number of threads in the pool")
                .baseUnit(THREADS)
                .register(registry);

        gauge("max", executor::getMaximumPoolSize)
                .description("The maximum number of threads in the pool")
                .baseUnit(THREADS)
                .register(registry);

        gauge("queued", () -> executor.getQueue().size())
                .description("The number of queued tasks")
                .baseUnit(TASKS)
                .register(registry);
    }

    private Gauge.Builder<Supplier<Number>> gauge(
            final String name,
            final Supplier<Number> supplier) {
        return Gauge.builder(metricName + "." + name, supplier)
                .tags(defaultTags);
    }

}

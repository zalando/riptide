package org.zalando.riptide.httpclient.metrics;

import com.google.common.collect.*;
import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.binder.*;
import org.apache.http.impl.conn.*;
import org.apache.http.pool.*;
import org.apiguardian.api.*;

import java.util.function.*;

import static com.google.common.base.Suppliers.*;
import static java.util.concurrent.TimeUnit.*;
import static org.apiguardian.api.API.Status.*;

@API(status = EXPERIMENTAL)
public final class HttpConnectionPoolMetrics implements MeterBinder {

    private final PoolingHttpClientConnectionManager manager;
    private final String metricName;
    private final ImmutableList<Tag> defaultTags;

    public HttpConnectionPoolMetrics(final PoolingHttpClientConnectionManager manager) {
        this(manager, "http.client.connections", ImmutableList.of());
    }

    @API(status = INTERNAL)
    HttpConnectionPoolMetrics(final PoolingHttpClientConnectionManager manager,
            final String metricName, final ImmutableList<Tag> defaultTags) {
        this.manager = manager;
        this.metricName = metricName;
        this.defaultTags = defaultTags;
    }

    public HttpConnectionPoolMetrics withMetricName(final String metricName) {
        return new HttpConnectionPoolMetrics(manager, metricName, defaultTags);
    }

    public HttpConnectionPoolMetrics withDefaultTags(final Tag... defaultTags) {
        return withDefaultTags(ImmutableList.copyOf(defaultTags));
    }

    public HttpConnectionPoolMetrics withDefaultTags(final Iterable<Tag> defaultTags) {
        return new HttpConnectionPoolMetrics(manager, metricName, ImmutableList.copyOf(defaultTags));
    }

    @Override
    public void bindTo(final MeterRegistry registry) {
        // since getTotalStats locks the connection pool, we cache the value for a minute to reduce possible contention
        final Supplier<PoolStats> stats = memoizeWithExpiration(manager::getTotalStats, 1, MINUTES);

        gauge(registry, "available", stats, PoolStats::getAvailable);
        gauge(registry, "leased", stats, PoolStats::getLeased);
        gauge(registry, "max", stats, PoolStats::getMax);
        gauge(registry, "pending", stats, PoolStats::getPending);
    }

    private void gauge(final MeterRegistry registry, final String name, final Supplier<PoolStats> stats,
            final Function<PoolStats, Number> function) {
        Gauge.builder(metricName + "." + name, () -> function.apply(stats.get()))
                .tags(defaultTags)
                .register(registry);
    }

}

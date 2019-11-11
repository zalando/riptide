package org.zalando.riptide.httpclient.metrics;

import com.google.common.collect.ImmutableList;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.MeterBinder;
import lombok.AllArgsConstructor;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.pool.PoolStats;
import org.apiguardian.api.API;

import java.util.function.Supplier;
import java.util.function.ToIntFunction;

import static com.google.common.base.Suppliers.memoizeWithExpiration;
import static com.google.common.collect.ImmutableList.copyOf;
import static java.util.concurrent.TimeUnit.MINUTES;
import static lombok.AccessLevel.PRIVATE;
import static org.apiguardian.api.API.Status.EXPERIMENTAL;

@API(status = EXPERIMENTAL)
@AllArgsConstructor(access = PRIVATE)
public final class HttpConnectionPoolMetrics implements MeterBinder {

    private static final String CONNECTIONS = "connections";
    private static final String REQUESTS = "requests";

    // since getTotalStats locks the connection pool, we cache the value for a minute to reduce possible contention
    private final Supplier<PoolStats> stats;
    private final String metricName;
    private final ImmutableList<Tag> defaultTags;

    public HttpConnectionPoolMetrics(final PoolingHttpClientConnectionManager manager) {
        this(manager, "http.client.connections", ImmutableList.of());
    }

    private HttpConnectionPoolMetrics(
            final PoolingHttpClientConnectionManager manager,
            final String metricName,
            final ImmutableList<Tag> defaultTags) {
        this(memoizeWithExpiration(manager::getTotalStats, 1, MINUTES),
                metricName, defaultTags);
    }

    public HttpConnectionPoolMetrics withMetricName(final String metricName) {
        return new HttpConnectionPoolMetrics(stats, metricName, defaultTags);
    }

    public HttpConnectionPoolMetrics withDefaultTags(final Tag... defaultTags) {
        return withDefaultTags(copyOf(defaultTags));
    }

    public HttpConnectionPoolMetrics withDefaultTags(final Iterable<Tag> defaultTags) {
        return new HttpConnectionPoolMetrics(stats, metricName, copyOf(defaultTags));
    }

    @Override
    public void bindTo(final MeterRegistry registry) {
        gauge("available", PoolStats::getAvailable)
                .description("The number idle connections")
                .baseUnit(CONNECTIONS)
                .register(registry);

        gauge("leased", PoolStats::getLeased)
                .description("The number of connections that are actively executing requests")
                .baseUnit(CONNECTIONS)
                .register(registry);

        gauge("total", stats -> stats.getAvailable() + stats.getLeased())
                .description("The number of connections that are currently in the pool")
                .baseUnit(CONNECTIONS)
                .register(registry);

        gauge("min", stats -> 0)
                .description("The minimum number of connections in the pool")
                .baseUnit(CONNECTIONS)
                .register(registry);

        gauge("max", PoolStats::getMax)
                .description("The maximum number of connections in the pool")
                .baseUnit(CONNECTIONS)
                .register(registry);

        gauge("queued", PoolStats::getPending)
                .description("The number of queued connection lease requests")
                .baseUnit(REQUESTS)
                .register(registry);
    }

    private Gauge.Builder<Supplier<Number>> gauge(
            final String name,
            final ToIntFunction<PoolStats> function) {

        return Gauge.builder(metricName + "." + name, () -> function.applyAsInt(stats.get()))
                .tags(defaultTags);
    }

}

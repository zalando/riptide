package org.zalando.riptide.autoconfigure;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import org.zalando.riptide.autoconfigure.RiptideProperties.Caching;
import org.zalando.riptide.autoconfigure.RiptideProperties.Caching.Heuristic;
import org.zalando.riptide.autoconfigure.RiptideProperties.OAuth;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.BinaryOperator;

import static com.google.common.collect.Maps.transformValues;
import static java.lang.Math.max;
import static java.util.Objects.requireNonNull;
import static org.zalando.riptide.autoconfigure.RiptideProperties.BackupRequest;
import static org.zalando.riptide.autoconfigure.RiptideProperties.CircuitBreaker;
import static org.zalando.riptide.autoconfigure.RiptideProperties.Client;
import static org.zalando.riptide.autoconfigure.RiptideProperties.Defaults;
import static org.zalando.riptide.autoconfigure.RiptideProperties.Retry;
import static org.zalando.riptide.autoconfigure.RiptideProperties.ThreadPool;

final class Defaulting {

    private Defaulting() {

    }

    static RiptideProperties withDefaults(final RiptideProperties base) {
        return merge(base, merge(base.getDefaults()));
    }

    private static Defaults merge(final Defaults defaults) {
        final int maxConnectionsPerRoute = defaults.getMaxConnectionsPerRoute();
        final int maxConnectionsTotal = max(defaults.getMaxConnectionsTotal(), maxConnectionsPerRoute);

        return new Defaults(
                defaults.getUrlResolution(),
                defaults.getConnectTimeout(),
                defaults.getSocketTimeout(),
                defaults.getConnectionTimeToLive(),
                maxConnectionsPerRoute,
                maxConnectionsTotal,
                merge(defaults.getThreadPool(), new ThreadPool(maxConnectionsTotal), Defaulting::merge),
                defaults.getOauth(),
                defaults.getDetectTransientFaults(),
                defaults.getPreserveStackTrace(),
                defaults.getRecordMetrics(),
                defaults.getRetry(),
                defaults.getCircuitBreaker(),
                defaults.getBackupRequest(),
                defaults.getTimeout(),
                defaults.getCaching()
        );
    }

    private static RiptideProperties merge(final RiptideProperties base, final Defaults defaults) {
        return new RiptideProperties(
                defaults,
                base.getOauth(),
                ImmutableMap.copyOf(transformValues(base.getClients(), client ->
                        merge(requireNonNull(client), defaults)))
        );
    }

    private static Client merge(final Client base, final Defaults defaults) {
        final int maxConnectionsPerRoute =
                either(base.getMaxConnectionsPerRoute(), defaults.getMaxConnectionsPerRoute());
        final int maxConnectionsTotal = max(maxConnectionsPerRoute,
                either(base.getMaxConnectionsTotal(), defaults.getMaxConnectionsTotal()));

        return new Client(
                base.getBaseUrl(),
                either(base.getUrlResolution(), defaults.getUrlResolution()),
                either(base.getConnectTimeout(), defaults.getConnectTimeout()),
                either(base.getSocketTimeout(), defaults.getSocketTimeout()),
                either(base.getConnectionTimeToLive(), defaults.getConnectionTimeToLive()),
                maxConnectionsPerRoute,
                maxConnectionsTotal,
                merge(base.getThreadPool(),
                        merge(new ThreadPool(null, maxConnectionsTotal, null, null), defaults.getThreadPool()),
                        Defaulting::merge),
                merge(base.getOauth(), defaults.getOauth(), Defaulting::merge),
                either(base.getDetectTransientFaults(), defaults.getDetectTransientFaults()),
                either(base.getPreserveStackTrace(), defaults.getPreserveStackTrace()),
                either(base.getRecordMetrics(), defaults.getRecordMetrics()),
                merge(base.getRetry(), defaults.getRetry(), Defaulting::merge),
                merge(base.getCircuitBreaker(), defaults.getCircuitBreaker(), Defaulting::merge),
                merge(base.getBackupRequest(), defaults.getBackupRequest(), Defaulting::merge),
                either(base.getTimeout(), defaults.getTimeout()),
                base.isCompressRequest(),
                base.getKeystore(),
                merge(base.getCaching(), defaults.getCaching(), Defaulting::merge)
        );
    }

    private static ThreadPool merge(final ThreadPool base, final ThreadPool defaults) {
        return new ThreadPool(
                either(base.getMinSize(), defaults.getMinSize()),
                either(base.getMaxSize(), defaults.getMaxSize()),
                either(base.getKeepAlive(), defaults.getKeepAlive()),
                either(base.getQueueSize(), defaults.getQueueSize())
        );
    }

    private static OAuth merge(final OAuth base, final OAuth defaults) {
        return new OAuth(
                either(base.getEnabled(), defaults.getEnabled())
        );
    }

    private static Retry merge(final Retry base, final Retry defaults) {
        return new Retry(
                either(base.getFixedDelay(), defaults.getFixedDelay()),
                merge(base.getBackoff(), defaults.getBackoff(), Defaulting::merge),
                either(base.getMaxRetries(), defaults.getMaxRetries()),
                either(base.getMaxDuration(), defaults.getMaxDuration()),
                either(base.getJitterFactor(), defaults.getJitterFactor()),
                either(base.getJitter(), defaults.getJitter())
        );
    }

    private static Retry.Backoff merge(final Retry.Backoff base, final Retry.Backoff defaults) {
        return new Retry.Backoff(
                either(base.getDelay(), defaults.getDelay()),
                either(base.getMaxDelay(), defaults.getMaxDelay()),
                either(base.getDelayFactor(), defaults.getDelayFactor())
        );
    }

    private static CircuitBreaker merge(final CircuitBreaker base, final CircuitBreaker defaults) {
        return new CircuitBreaker(
                either(base.getFailureThreshold(), defaults.getFailureThreshold()),
                either(base.getDelay(), defaults.getDelay()),
                either(base.getSuccessThreshold(), defaults.getSuccessThreshold())
        );
    }

    private static BackupRequest merge(final BackupRequest base, final BackupRequest defaults) {
        return new BackupRequest(
                either(base.getDelay(), defaults.getDelay())
        );
    }

    @VisibleForTesting
    static Caching merge(final Caching base, final Caching defaults) {
        return new Caching(
                either(base.getShared(), defaults.getShared()),
                either(base.getDirectory(), defaults.getDirectory()),
                either(base.getMaxObjectSize(), defaults.getMaxObjectSize()),
                either(base.getMaxCacheEntries(), defaults.getMaxCacheEntries()),
                merge(base.getHeuristic(), defaults.getHeuristic(), Defaulting::merge)
        );
    }

    private static Heuristic merge(final Heuristic base, final Heuristic defaults) {
        return new Heuristic(
                either(base.getCoefficient(), defaults.getCoefficient()),
                either(base.getDefaultLifeTime(), defaults.getDefaultLifeTime())
        );
    }

    @SafeVarargs
    private static <T> T either(final T... options) {
        return Arrays.stream(options).filter(Objects::nonNull).findFirst().orElse(null);
    }

    private static <T> T merge(@Nullable final T base, @Nullable final T defaults, final BinaryOperator<T> merger) {
        if (base == null && defaults == null) {
            return null;
        } else if (base == null) {
            return defaults;
        } else if (defaults == null) {
            return base;
        } else {
            return merger.apply(base, defaults);
        }
    }

}

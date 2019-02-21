package org.zalando.riptide.autoconfigure;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import org.zalando.riptide.autoconfigure.RiptideProperties.Caching;
import org.zalando.riptide.autoconfigure.RiptideProperties.Caching.Heuristic;
import org.zalando.riptide.autoconfigure.RiptideProperties.Connections;
import org.zalando.riptide.autoconfigure.RiptideProperties.Metrics;
import org.zalando.riptide.autoconfigure.RiptideProperties.OAuth;
import org.zalando.riptide.autoconfigure.RiptideProperties.RequestCompression;
import org.zalando.riptide.autoconfigure.RiptideProperties.StackTracePreservation;
import org.zalando.riptide.autoconfigure.RiptideProperties.TransientFaultDetection;

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
import static org.zalando.riptide.autoconfigure.RiptideProperties.Threads;

final class Defaulting {

    private Defaulting() {

    }

    static RiptideProperties withDefaults(final RiptideProperties base) {
        return merge(base, merge(base.getDefaults()));
    }

    private static Defaults merge(final Defaults defaults) {
        final Connections connections = merge(
                new Connections(null, null, null, null, max(
                        defaults.getConnections().getMaxTotal(),
                        defaults.getConnections().getMaxPerRoute())),
                defaults.getConnections());

        return new Defaults(
                defaults.getUrlResolution(),
                connections,
                merge(defaults.getThreads(), new Threads(connections.getMaxTotal())),
                defaults.getOauth(),
                defaults.getTransientFaultDetection(),
                defaults.getStackTracePreservation(),
                defaults.getMetrics(),
                defaults.getRetry(),
                defaults.getCircuitBreaker(),
                defaults.getBackupRequest(),
                defaults.getTimeout(),
                defaults.getRequestCompression(),
                defaults.getCaching()
        );
    }

    private static RiptideProperties merge(final RiptideProperties base, final Defaults defaults) {
        return new RiptideProperties(
                defaults,
                ImmutableMap.copyOf(transformValues(base.getClients(), client ->
                        merge(requireNonNull(client), defaults)))
        );
    }

    private static Client merge(final Client base, final Defaults defaults) {
        final Connections connections = merge(base.getConnections(), defaults.getConnections(), Defaulting::merge);

        return new Client(
                base.getBaseUrl(),
                either(base.getUrlResolution(), defaults.getUrlResolution()),
                connections,
                merge(base.getThreads(),
                        merge(new Threads(connections.getMaxTotal()), defaults.getThreads()),
                        Defaulting::merge),
                merge(base.getOauth(), defaults.getOauth(), Defaulting::merge),
                merge(base.getTransientFaultDetection(), defaults.getTransientFaultDetection(), Defaulting::merge),
                merge(base.getStackTracePreservation(), defaults.getStackTracePreservation(), Defaulting::merge),
                merge(base.getMetrics(), defaults.getMetrics(), Defaulting::merge),
                merge(base.getRetry(), defaults.getRetry(), Defaulting::merge),
                merge(base.getCircuitBreaker(), defaults.getCircuitBreaker(), Defaulting::merge),
                merge(base.getBackupRequest(), defaults.getBackupRequest(), Defaulting::merge),
                either(base.getTimeout(), defaults.getTimeout()),
                merge(base.getRequestCompression(), defaults.getRequestCompression(), Defaulting::merge),
                base.getKeystore(),
                merge(base.getCaching(), defaults.getCaching(), Defaulting::merge)
        );
    }

    private static Connections merge(final Connections base, final Connections defaults) {
        final int maxPerRoute = either(base.getMaxPerRoute(), defaults.getMaxPerRoute());
        final int maxTotal = max(maxPerRoute, either(base.getMaxTotal(), defaults.getMaxTotal()));

        return new Connections(
                either(base.getConnectTimeout(), defaults.getConnectTimeout()),
                either(base.getSocketTimeout(), defaults.getSocketTimeout()),
                either(base.getTimeToLive(), defaults.getTimeToLive()),
                maxPerRoute,
                maxTotal
        );
    }

    private static Threads merge(final Threads base, final Threads defaults) {
        return new Threads(
                either(base.getMinSize(), defaults.getMinSize()),
                either(base.getMaxSize(), defaults.getMaxSize()),
                either(base.getKeepAlive(), defaults.getKeepAlive()),
                either(base.getQueueSize(), defaults.getQueueSize())
        );
    }

    private static OAuth merge(final OAuth base, final OAuth defaults) {
        return new OAuth(
                either(base.getEnabled(), defaults.getEnabled()),
                either(base.getCredentialsDirectory(), defaults.getCredentialsDirectory())
        );
    }

    private static TransientFaultDetection merge(final TransientFaultDetection base,
            final TransientFaultDetection defaults) {
        return new TransientFaultDetection(
                either(base.getEnabled(), defaults.getEnabled())
        );
    }

    private static StackTracePreservation merge(final StackTracePreservation base,
            final StackTracePreservation defaults) {
        return new StackTracePreservation(
                either(base.getEnabled(), defaults.getEnabled())
        );
    }

    private static Metrics merge(final Metrics base, final Metrics defaults) {
        return new Metrics(
                either(base.getEnabled(), defaults.getEnabled())
        );
    }

    private static Retry merge(final Retry base, final Retry defaults) {
        return new Retry(
                either(base.getEnabled(), defaults.getEnabled()),
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
                either(base.getEnabled(), defaults.getEnabled()),
                either(base.getDelay(), defaults.getDelay()),
                either(base.getMaxDelay(), defaults.getMaxDelay()),
                either(base.getDelayFactor(), defaults.getDelayFactor())
        );
    }

    private static CircuitBreaker merge(final CircuitBreaker base, final CircuitBreaker defaults) {
        return new CircuitBreaker(
                either(base.getEnabled(), defaults.getEnabled()),
                either(base.getFailureThreshold(), defaults.getFailureThreshold()),
                either(base.getDelay(), defaults.getDelay()),
                either(base.getSuccessThreshold(), defaults.getSuccessThreshold())
        );
    }

    private static BackupRequest merge(final BackupRequest base, final BackupRequest defaults) {
        return new BackupRequest(
                either(base.getEnabled(), defaults.getEnabled()),
                either(base.getDelay(), defaults.getDelay())
        );
    }

    private static RequestCompression merge(final RequestCompression base, final RequestCompression defaults) {
        return new RequestCompression(
                either(base.getEnabled(), defaults.getEnabled())
        );
    }

    @VisibleForTesting
    static Caching merge(final Caching base, final Caching defaults) {
        return new Caching(
                either(base.getEnabled(), defaults.getEnabled()),
                either(base.getShared(), defaults.getShared()),
                either(base.getDirectory(), defaults.getDirectory()),
                either(base.getMaxObjectSize(), defaults.getMaxObjectSize()),
                either(base.getMaxCacheEntries(), defaults.getMaxCacheEntries()),
                merge(base.getHeuristic(), defaults.getHeuristic(), Defaulting::merge)
        );
    }

    private static Heuristic merge(final Heuristic base, final Heuristic defaults) {
        return new Heuristic(
                either(base.getEnabled(), defaults.getEnabled()),
                either(base.getCoefficient(), defaults.getCoefficient()),
                either(base.getDefaultLifeTime(), defaults.getDefaultLifeTime())
        );
    }

    @SafeVarargs
    private static <T> T either(final T... options) {
        return Arrays.stream(options).filter(Objects::nonNull).findFirst().orElse(null);
    }

    private static <T> T merge(@Nullable final T base, final T defaults, final BinaryOperator<T> merger) {
        if (base == null) {
            return defaults;
        } else {
            return merger.apply(base, defaults);
        }
    }

}

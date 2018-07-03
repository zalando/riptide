package org.zalando.riptide.spring;

import com.google.common.collect.ImmutableMap;
import org.zalando.riptide.UrlResolution;
import org.zalando.riptide.spring.RiptideProperties.BackupRequest;
import org.zalando.riptide.spring.RiptideProperties.Client;
import org.zalando.riptide.spring.RiptideProperties.Defaults;
import org.zalando.riptide.spring.RiptideProperties.GlobalOAuth;
import org.zalando.riptide.spring.RiptideProperties.Retry.Backoff;
import org.zalando.riptide.spring.RiptideProperties.ThreadPool;

import javax.annotation.Nullable;
import java.net.URI;
import java.util.Optional;
import java.util.function.BinaryOperator;

import static com.google.common.collect.Maps.transformValues;
import static java.lang.Math.max;
import static java.lang.System.getenv;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.zalando.riptide.spring.RiptideProperties.CircuitBreaker;
import static org.zalando.riptide.spring.RiptideProperties.Retry;

final class Defaulting {

    private Defaulting() {

    }

    static RiptideProperties withDefaults(final RiptideProperties base) {
        return merge(base, merge(base.getDefaults()));
    }

    private static Defaults merge(final Defaults defaults) {
        final int maxConnectionsPerRoute = either(defaults.getMaxConnectionsPerRoute(), 20);
        final int maxConnectionsTotal = max(either(defaults.getMaxConnectionsTotal(), 20), maxConnectionsPerRoute);

        return new Defaults(
                either(defaults.getUrlResolution(), UrlResolution.RFC),
                either(defaults.getConnectTimeout(), TimeSpan.of(5, SECONDS)),
                either(defaults.getSocketTimeout(), TimeSpan.of(5, SECONDS)),
                either(defaults.getConnectionTimeToLive(), TimeSpan.of(30, SECONDS)),
                maxConnectionsPerRoute,
                maxConnectionsTotal,
                merge(defaults.getThreadPool(), new ThreadPool(
                                1, maxConnectionsTotal,
                                TimeSpan.of(1, MINUTES),
                                0),
                        Defaulting::merge),
                either(defaults.getDetectTransientFaults(), false),
                either(defaults.getPreserveStackTrace(), true),
                either(defaults.getRecordMetrics(), false),
                defaults.getRetry(),
                defaults.getCircuitBreaker(),
                defaults.getBackupRequest(),
                defaults.getTimeout()
        );
    }

    private static RiptideProperties merge(final RiptideProperties base, final Defaults defaults) {
        return new RiptideProperties(
                defaults,
                merge(base.getOauth(), defaults),
                ImmutableMap.copyOf(transformValues(base.getClients(), client ->
                        merge(requireNonNull(client), defaults)))
        );
    }

    private static GlobalOAuth merge(final GlobalOAuth base, final Defaults defaults) {
        return new GlobalOAuth(
                either(base.getAccessTokenUrl(),
                        Optional.ofNullable(getenv("ACCESS_TOKEN_URL")).map(URI::create).orElse(null)),
                base.getCredentialsDirectory(),
                base.getSchedulingPeriod(),
                either(base.getConnectTimeout(), defaults.getConnectTimeout()),
                either(base.getSocketTimeout(), defaults.getSocketTimeout())
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
                base.getOauth(),
                either(base.getDetectTransientFaults(), defaults.getDetectTransientFaults()),
                either(base.getPreserveStackTrace(), defaults.getPreserveStackTrace()),
                either(base.getRecordMetrics(), defaults.getRecordMetrics()),
                merge(base.getRetry(), defaults.getRetry(), Defaulting::merge),
                merge(base.getCircuitBreaker(), defaults.getCircuitBreaker(), Defaulting::merge),
                merge(base.getBackupRequest(), defaults.getBackupRequest(), Defaulting::merge),
                either(base.getTimeout(), defaults.getTimeout()),
                base.isCompressRequest(),
                base.getKeystore()
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

    private static Backoff merge(final Backoff base, final Backoff defaults) {
        return new Backoff(
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

    private static <T> T either(@Nullable final T left, @Nullable final T right) {
        return Optional.ofNullable(left).orElse(right);
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

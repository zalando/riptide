package org.zalando.riptide.autoconfigure;

import com.google.common.collect.ImmutableMap;
import org.zalando.riptide.UrlResolution;

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

final class Defaulting {

    private Defaulting() {

    }

    static RiptideProperties withDefaults(final RiptideProperties base) {
        return merge(base, merge(base.getDefaults()));
    }

    private static RiptideProperties.Defaults merge(final RiptideProperties.Defaults defaults) {
        final int maxConnectionsPerRoute = either(defaults.getMaxConnectionsPerRoute(), 20);
        final int maxConnectionsTotal = max(either(defaults.getMaxConnectionsTotal(), 20), maxConnectionsPerRoute);

        return new RiptideProperties.Defaults(
                either(defaults.getUrlResolution(), UrlResolution.RFC),
                either(defaults.getConnectTimeout(), TimeSpan.of(5, SECONDS)),
                either(defaults.getSocketTimeout(), TimeSpan.of(5, SECONDS)),
                either(defaults.getConnectionTimeToLive(), TimeSpan.of(30, SECONDS)),
                maxConnectionsPerRoute,
                maxConnectionsTotal,
                merge(defaults.getThreadPool(), new RiptideProperties.ThreadPool(
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

    private static RiptideProperties merge(final RiptideProperties base, final RiptideProperties.Defaults defaults) {
        return new RiptideProperties(
                defaults,
                merge(base.getOauth(), defaults),
                ImmutableMap.copyOf(transformValues(base.getClients(), client ->
                        merge(requireNonNull(client), defaults)))
        );
    }

    private static RiptideProperties.GlobalOAuth merge(final RiptideProperties.GlobalOAuth base, final RiptideProperties.Defaults defaults) {
        return new RiptideProperties.GlobalOAuth(
                either(base.getAccessTokenUrl(),
                        Optional.ofNullable(getenv("ACCESS_TOKEN_URL")).map(URI::create).orElse(null)),
                base.getCredentialsDirectory(),
                base.getSchedulingPeriod(),
                either(base.getConnectTimeout(), defaults.getConnectTimeout()),
                either(base.getSocketTimeout(), defaults.getSocketTimeout())
        );
    }

    private static RiptideProperties.Client merge(final RiptideProperties.Client base, final RiptideProperties.Defaults defaults) {
        final int maxConnectionsPerRoute =
                either(base.getMaxConnectionsPerRoute(), defaults.getMaxConnectionsPerRoute());
        final int maxConnectionsTotal = max(maxConnectionsPerRoute,
                either(base.getMaxConnectionsTotal(), defaults.getMaxConnectionsTotal()));

        return new RiptideProperties.Client(
                base.getBaseUrl(),
                either(base.getUrlResolution(), defaults.getUrlResolution()),
                either(base.getConnectTimeout(), defaults.getConnectTimeout()),
                either(base.getSocketTimeout(), defaults.getSocketTimeout()),
                either(base.getConnectionTimeToLive(), defaults.getConnectionTimeToLive()),
                maxConnectionsPerRoute,
                maxConnectionsTotal,
                merge(base.getThreadPool(),
                        merge(new RiptideProperties.ThreadPool(null, maxConnectionsTotal, null, null), defaults.getThreadPool()),
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

    private static RiptideProperties.ThreadPool merge(final RiptideProperties.ThreadPool base, final RiptideProperties.ThreadPool defaults) {
        return new RiptideProperties.ThreadPool(
                either(base.getMinSize(), defaults.getMinSize()),
                either(base.getMaxSize(), defaults.getMaxSize()),
                either(base.getKeepAlive(), defaults.getKeepAlive()),
                either(base.getQueueSize(), defaults.getQueueSize())
        );
    }

    private static RiptideProperties.Retry merge(final RiptideProperties.Retry base, final RiptideProperties.Retry defaults) {
        return new RiptideProperties.Retry(
                either(base.getFixedDelay(), defaults.getFixedDelay()),
                merge(base.getBackoff(), defaults.getBackoff(), Defaulting::merge),
                either(base.getMaxRetries(), defaults.getMaxRetries()),
                either(base.getMaxDuration(), defaults.getMaxDuration()),
                either(base.getJitterFactor(), defaults.getJitterFactor()),
                either(base.getJitter(), defaults.getJitter())
        );
    }

    private static RiptideProperties.Retry.Backoff merge(final RiptideProperties.Retry.Backoff base, final RiptideProperties.Retry.Backoff defaults) {
        return new RiptideProperties.Retry.Backoff(
                either(base.getDelay(), defaults.getDelay()),
                either(base.getMaxDelay(), defaults.getMaxDelay()),
                either(base.getDelayFactor(), defaults.getDelayFactor())
        );
    }

    private static RiptideProperties.CircuitBreaker merge(final RiptideProperties.CircuitBreaker base, final RiptideProperties.CircuitBreaker defaults) {
        return new RiptideProperties.CircuitBreaker(
                either(base.getFailureThreshold(), defaults.getFailureThreshold()),
                either(base.getDelay(), defaults.getDelay()),
                either(base.getSuccessThreshold(), defaults.getSuccessThreshold())
        );
    }

    private static RiptideProperties.BackupRequest merge(final RiptideProperties.BackupRequest base, final RiptideProperties.BackupRequest defaults) {
        return new RiptideProperties.BackupRequest(
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

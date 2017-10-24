package org.zalando.riptide.spring;

import com.google.common.collect.ImmutableMap;
import org.zalando.riptide.UrlResolution;
import org.zalando.riptide.spring.RiptideSettings.Client;
import org.zalando.riptide.spring.RiptideSettings.Defaults;
import org.zalando.riptide.spring.RiptideSettings.GlobalOAuth;
import org.zalando.riptide.spring.RiptideSettings.Retry.Backoff;

import javax.annotation.Nullable;
import java.net.URI;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BinaryOperator;

import static com.google.common.collect.Maps.transformValues;
import static java.lang.Math.max;
import static java.lang.System.getenv;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.zalando.riptide.spring.RiptideSettings.CircuitBreaker;
import static org.zalando.riptide.spring.RiptideSettings.Retry;

final class Defaulting {

    static RiptideSettings withDefaults(final RiptideSettings base) {
        return merge(base, merge(base.getDefaults()));
    }

    private static Defaults merge(final Defaults defaults) {
        return new Defaults(
                either(defaults.getUrlResolution(), UrlResolution.RFC),
                either(defaults.getConnectTimeout(), TimeSpan.of(5, SECONDS)),
                either(defaults.getSocketTimeout(), TimeSpan.of(5, SECONDS)),
                either(defaults.getConnectionTimeToLive(), TimeSpan.of(30, SECONDS)),
                either(defaults.getMaxConnectionsPerRoute(), 20),
                either(defaults.getMaxConnectionsTotal(), 20),
                either(defaults.getDetectTransientFaults(), false),
                either(defaults.getPreserveStackTrace(), true),
                either(defaults.getRecordMetrics(), false),
                defaults.getRetry(),
                defaults.getCircuitBreaker(),
                defaults.getTimeout()
        );
    }

    private static RiptideSettings merge(final RiptideSettings base, final Defaults defaults) {
        return new RiptideSettings(
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

    static Client merge(final Client base, final Defaults defaults) {
        final int maxConnectionsPerRoute =
                either(base.getMaxConnectionsPerRoute(), defaults.getMaxConnectionsPerRoute());

        final int maxConnectionsTotal =
                either(base.getMaxConnectionsTotal(), defaults.getMaxConnectionsTotal());

        return new Client(
                base.getBaseUrl(),
                either(base.getUrlResolution(), defaults.getUrlResolution()),
                either(base.getConnectTimeout(), defaults.getConnectTimeout()),
                either(base.getSocketTimeout(), defaults.getSocketTimeout()),
                either(base.getConnectionTimeToLive(), defaults.getConnectionTimeToLive()),
                maxConnectionsPerRoute,
                max(maxConnectionsPerRoute, maxConnectionsTotal),
                base.getOauth(),
                either(base.getDetectTransientFaults(), defaults.getDetectTransientFaults()),
                either(base.getPreserveStackTrace(), defaults.getPreserveStackTrace()),
                either(base.getRecordMetrics(), defaults.getRecordMetrics()),
                merge(base.getRetry(), defaults.getRetry(), Defaulting::merge),
                merge(base.getCircuitBreaker(), defaults.getCircuitBreaker(), Defaulting::merge),
                either(base.getTimeout(), defaults.getTimeout()),
                base.isCompressRequest(),
                base.getKeystore()
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

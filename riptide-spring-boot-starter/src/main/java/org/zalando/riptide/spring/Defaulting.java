package org.zalando.riptide.spring;

import com.google.common.collect.ImmutableMap;
import org.zalando.riptide.spring.RiptideSettings.Client;
import org.zalando.riptide.spring.RiptideSettings.Defaults;
import org.zalando.riptide.spring.RiptideSettings.Failsafe;
import org.zalando.riptide.spring.RiptideSettings.Failsafe.Retry.Backoff;
import org.zalando.riptide.spring.RiptideSettings.GlobalOAuth;

import javax.annotation.Nullable;
import java.net.URI;
import java.util.Optional;
import java.util.function.BinaryOperator;

import static com.google.common.collect.Maps.transformValues;
import static java.lang.Math.max;
import static java.lang.System.getenv;
import static org.zalando.riptide.spring.RiptideSettings.Failsafe.CircuitBreaker;
import static org.zalando.riptide.spring.RiptideSettings.Failsafe.Retry;

final class Defaulting {

    static RiptideSettings withDefaults(final RiptideSettings base) {
        return merge(base, base.getDefaults());
    }

    private static RiptideSettings merge(final RiptideSettings base, final Defaults defaults) {
        return new RiptideSettings(
                defaults,
                merge(base.getOauth(), defaults),
                ImmutableMap.copyOf(transformValues(base.getClients(), client ->
                        merge(client, defaults)
                ))
        );
    }

    private static GlobalOAuth merge(final GlobalOAuth base, final Defaults defaults) {
        return new GlobalOAuth(
                either(base.getAccessTokenUrl(),
                        Optional.ofNullable(getenv("ACCESS_TOKEN_URL")).map(URI::create).orElse(null)),
                base.getCredentialsDirectory(),
                base.getSchedulingPeriod(),
                either(base.getConnectionTimeout(), defaults.getConnectionTimeout()),
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
                either(base.getConnectionTimeout(), defaults.getConnectionTimeout()),
                either(base.getSocketTimeout(), defaults.getSocketTimeout()),
                either(base.getConnectionTimeToLive(), defaults.getConnectionTimeToLive()),
                maxConnectionsPerRoute,
                max(maxConnectionsPerRoute, maxConnectionsTotal),
                base.getOauth(),
                either(base.getKeepOriginalStackTrace(), defaults.isKeepOriginalStackTrace()),
                either(base.getDetectTransientFaults(), defaults.isDetectTransientFaults()),
                merge(base.getFailsafe(), defaults.getFailsafe(), Defaulting::merge),
                base.isCompressRequest(),
                base.getKeystore(),
                either(base.getTimeout(), defaults.getTimeout())
        );
    }

    private static Failsafe merge(final Failsafe base, final Failsafe defaults) {
        return new Failsafe(
                merge(base.getRetry(), defaults.getRetry(), Defaulting::merge),
                merge(base.getCircuitBreaker(), defaults.getCircuitBreaker(), Defaulting::merge)
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

package org.zalando.riptide.spring;

import com.google.common.collect.ImmutableMap;
import org.zalando.riptide.spring.RiptideSettings.Client;
import org.zalando.riptide.spring.RiptideSettings.Defaults;
import org.zalando.riptide.spring.RiptideSettings.Failsafe;
import org.zalando.riptide.spring.RiptideSettings.Failsafe.Retry.ExponentialBackoff;
import org.zalando.riptide.spring.RiptideSettings.GlobalOAuth;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.BinaryOperator;

import static com.google.common.collect.Maps.transformValues;
import static java.lang.Math.max;
import static org.zalando.riptide.spring.RiptideSettings.Failsafe.CircuitBreaker;
import static org.zalando.riptide.spring.RiptideSettings.Failsafe.Retry;

final class Defaulting {

    static RiptideSettings withDefaults(final RiptideSettings original) {
        return new RiptideSettings(
                original.getDefaults(),
                merge(original.getOauth(), original.getDefaults()),
                ImmutableMap.copyOf(transformValues(original.getClients(), client ->
                        merge(client, original.getDefaults())
                ))
        );
    }

    private static GlobalOAuth merge(final GlobalOAuth auth, final Defaults defaults) {
        return new GlobalOAuth(
                auth.getAccessTokenUrl(),
                auth.getCredentialsDirectory(),
                auth.getSchedulingPeriod(),
                either(auth.getConnectionTimeout(), defaults.getConnectionTimeout()),
                either(auth.getSocketTimeout(), defaults.getSocketTimeout())
        );
    }

    static Client merge(final Client client, final Defaults defaults) {
        final int maxConnectionsPerRoute =
                either(client.getMaxConnectionsPerRoute(), defaults.getMaxConnectionsPerRoute());

        final int maxConnectionsTotal =
                either(client.getMaxConnectionsTotal(), defaults.getMaxConnectionsTotal());

        return new Client(
                client.getBaseUrl(),
                either(client.getConnectionTimeout(), defaults.getConnectionTimeout()),
                either(client.getSocketTimeout(), defaults.getSocketTimeout()),
                either(client.getConnectionTimeToLive(), defaults.getConnectionTimeToLive()),
                maxConnectionsPerRoute,
                max(maxConnectionsPerRoute, maxConnectionsTotal),
                client.getOauth(),
                either(client.getKeepOriginalStackTrace(), defaults.isKeepOriginalStackTrace()),
                either(client.getDetectTransientFaults(), defaults.isDetectTransientFaults()),
                merge(client.getFailsafe(), defaults.getFailsafe()),
                client.isCompressRequest(),
                client.getKeystore()
        );
    }

    @Nullable
    private static Failsafe merge(@Nullable final Failsafe l, @Nullable final Failsafe r) {
        return merge(l, r, (base, defaults) -> new Failsafe(
                merge(base.getRetry(), defaults.getRetry()),
                merge(base.getCircuitBreaker(), defaults.getCircuitBreaker())
        ));
    }

    @Nullable
    private static Retry merge(@Nullable final Retry l, @Nullable final Retry r) {
        return merge(l, r, (base, defaults) -> new Retry(
                either(base.getFixedDelay(), defaults.getFixedDelay()),
                merge(base.getExponentialBackoff(), defaults.getExponentialBackoff()),
                either(base.getMaxRetries(), defaults.getMaxRetries()),
                either(base.getMaxDuration(), defaults.getMaxDuration()),
                either(base.getJitterFactor(), defaults.getJitterFactor()),
                either(base.getJitter(), defaults.getJitter())
        ));
    }

    @Nullable
    private static ExponentialBackoff merge(@Nullable final ExponentialBackoff l,
            @Nullable final ExponentialBackoff r) {
        return merge(l, r, (base, defaults) -> new ExponentialBackoff(
                either(base.getDelay(), defaults.getDelay()),
                either(base.getMaxDelay(), defaults.getMaxDelay()),
                either(base.getDelayFactor(), defaults.getDelayFactor())
        ));
    }

    @Nullable
    private static CircuitBreaker merge(@Nullable final CircuitBreaker l, @Nullable final CircuitBreaker r) {
        return merge(l, r, (base, defaults) -> new CircuitBreaker(
                either(base.getFailureThreshold(), defaults.getFailureThreshold()),
                either(base.getDelay(), defaults.getDelay()),
                either(base.getSuccessThreshold(), defaults.getSuccessThreshold())
        ));
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

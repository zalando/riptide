package org.zalando.riptide.autoconfigure;

import com.google.common.annotations.*;
import com.google.common.collect.*;
import org.zalando.riptide.autoconfigure.RiptideProperties.*;
import org.zalando.riptide.autoconfigure.RiptideProperties.Caching.*;
import org.zalando.riptide.autoconfigure.RiptideProperties.CertificatePinning.*;
import org.zalando.riptide.autoconfigure.RiptideProperties.Chaos.*;
import org.zalando.riptide.autoconfigure.RiptideProperties.Retry.*;

import javax.annotation.*;
import java.util.*;
import java.util.function.*;

import static com.google.common.collect.Maps.*;
import static java.lang.Math.*;
import static java.util.Objects.*;
import static org.zalando.riptide.autoconfigure.RiptideProperties.*;

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
                        defaults.getConnections().getMaxPerRoute()), null),
                defaults.getConnections());

        return new Defaults(
                defaults.getUrlResolution(),
                connections,
                merge(defaults.getThreads(), new Threads(connections.getMaxTotal())),
                defaults.getOauth(),
                defaults.getTransientFaultDetection(),
                defaults.getStackTracePreservation(),
                defaults.getMetrics(),
                defaults.getLogging(),
                defaults.getRetry(),
                defaults.getCircuitBreaker(),
                defaults.getBackupRequest(),
                defaults.getTimeouts(),
                defaults.getRequestCompression(),
                defaults.getCertificatePinning(),
                defaults.getCaching(),
                defaults.getTracing(),
                defaults.getChaos(),
                defaults.getSoap()
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
                merge(base.getLogging(), defaults.getLogging(), Defaulting::merge),
                merge(base.getRetry(), defaults.getRetry(), Defaulting::merge),
                merge(base.getCircuitBreaker(), defaults.getCircuitBreaker(), Defaulting::merge),
                merge(base.getBackupRequest(), defaults.getBackupRequest(), Defaulting::merge),
                merge(base.getTimeouts(), defaults.getTimeouts(), Defaulting::merge),
                merge(base.getRequestCompression(), defaults.getRequestCompression(), Defaulting::merge),
                merge(base.getCertificatePinning(), defaults.getCertificatePinning(), Defaulting::merge),
                merge(base.getCaching(), defaults.getCaching(), Defaulting::merge),
                merge(base.getTracing(), defaults.getTracing(), Defaulting::merge),
                merge(base.getChaos(), defaults.getChaos(), Defaulting::merge),
                merge(base.getSoap(), defaults.getSoap(), Defaulting::merge)
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
                maxTotal,
                either(base.getMode(), defaults.getMode())
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

    private static Logging merge(final Logging base, final Logging defaults) {
        return new Logging(
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

    private static Backoff merge(final Backoff base, final Backoff defaults) {
        return new Backoff(
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

    private static Timeouts merge(final Timeouts base, final Timeouts defaults) {
        return new Timeouts(
                either(base.getEnabled(), defaults.getEnabled()),
                either(base.getGlobal(), defaults.getGlobal())
        );
    }

    private static RequestCompression merge(final RequestCompression base, final RequestCompression defaults) {
        return new RequestCompression(
                either(base.getEnabled(), defaults.getEnabled())
        );
    }

    private static CertificatePinning merge(final CertificatePinning base, final CertificatePinning defaults) {
        return new CertificatePinning(
                either(base.getEnabled(), defaults.getEnabled()),
                merge(base.getKeystore(), defaults.getKeystore(), Defaulting::merge)
        );
    }

    private static Keystore merge(final Keystore base, final Keystore defaults) {
        return new Keystore(
                either(base.getPath(), defaults.getPath()),
                either(base.getPassword(), defaults.getPassword())
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

    private static Tracing merge(final Tracing base, final Tracing defaults) {
        final boolean enabled = either(base.getEnabled(), defaults.getEnabled());
        final boolean propagateFlowId = either(base.getPropagateFlowId(), defaults.getPropagateFlowId());

        return new Tracing(
                enabled,
                merge(base.getTags(), defaults.getTags(), Defaulting::merge),
                enabled && propagateFlowId
        );
    }

    private static  <K, V> Map<K, V> merge(final Map<K, V> base, final Map<K, V> defaults) {
        final Map<K, V> map = new HashMap<>();
        map.putAll(defaults);
        map.putAll(base);
        return map;
    }

    private static Chaos merge(final Chaos base, final Chaos defaults) {
        return new Chaos(
                merge(base.getLatency(), defaults.getLatency(), Defaulting::merge),
                merge(base.getExceptions(), defaults.getExceptions(), Defaulting::merge),
                merge(base.getErrorResponses(), defaults.getErrorResponses(), Defaulting::merge)
        );
    }

    private static Latency merge(final Latency base, final Latency defaults) {
        return new Latency(
                either(base.getEnabled(), defaults.getEnabled()),
                either(base.getProbability(), defaults.getProbability()),
                either(base.getDelay(), defaults.getDelay())
        );
    }

    private static Exceptions merge(final Exceptions base, final Exceptions defaults) {
        return new Exceptions(
                either(base.getEnabled(), defaults.getEnabled()),
                either(base.getProbability(), defaults.getProbability())
        );
    }

    private static ErrorResponses merge(final ErrorResponses base, final ErrorResponses defaults) {
        return new ErrorResponses(
                either(base.getEnabled(), defaults.getEnabled()),
                either(base.getProbability(), defaults.getProbability()),
                either(base.getStatusCodes(), defaults.getStatusCodes())
        );
    }

    private static Soap merge(final Soap base, final Soap defaults) {
        return new Soap(
                either(base.getEnabled(), defaults.getEnabled()),
                either(base.getProtocol(), defaults.getProtocol())
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

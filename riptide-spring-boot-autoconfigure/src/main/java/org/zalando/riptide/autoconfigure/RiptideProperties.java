package org.zalando.riptide.autoconfigure;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.http.impl.client.cache.CacheConfig;
import org.apiguardian.api.API;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.zalando.riptide.UrlResolution;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apiguardian.api.API.Status.INTERNAL;

@API(status = INTERNAL)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ConfigurationProperties(prefix = "riptide")
public final class RiptideProperties {

    private Defaults defaults = new Defaults();
    private GlobalOAuth oauth = new GlobalOAuth();
    private Map<String, Client> clients = new LinkedHashMap<>();

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static final class Defaults {
        private UrlResolution urlResolution = UrlResolution.RFC;
        private TimeSpan connectTimeout = TimeSpan.of(5, SECONDS);
        private TimeSpan socketTimeout = TimeSpan.of(5, SECONDS);
        private TimeSpan connectionTimeToLive = TimeSpan.of(30, SECONDS);
        private Integer maxConnectionsPerRoute = 20;
        private Integer maxConnectionsTotal = 20;
        @NestedConfigurationProperty
        private ThreadPool threadPool;
        @NestedConfigurationProperty
        private OAuth oauth = new OAuth();
        private Boolean detectTransientFaults = Boolean.FALSE;
        private Boolean preserveStackTrace = Boolean.TRUE;
        private Boolean recordMetrics = Boolean.FALSE;
        @NestedConfigurationProperty
        private Retry retry;
        @NestedConfigurationProperty
        private CircuitBreaker circuitBreaker;
        @NestedConfigurationProperty
        private BackupRequest backupRequest;
        private TimeSpan timeout;
        @NestedConfigurationProperty
        private Caching caching;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static final class GlobalOAuth {
        private Path credentialsDirectory = Paths.get("/meta/credentials");
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static final class Client {
        private String baseUrl;
        private UrlResolution urlResolution;
        private TimeSpan connectTimeout;
        private TimeSpan socketTimeout;
        private TimeSpan connectionTimeToLive;
        private Integer maxConnectionsPerRoute;
        private Integer maxConnectionsTotal;
        @NestedConfigurationProperty
        private ThreadPool threadPool;
        @NestedConfigurationProperty
        private OAuth oauth = new OAuth();
        private Boolean detectTransientFaults;
        private Boolean preserveStackTrace;
        private Boolean recordMetrics;
        @NestedConfigurationProperty
        private Retry retry;
        @NestedConfigurationProperty
        private CircuitBreaker circuitBreaker;
        @NestedConfigurationProperty
        private BackupRequest backupRequest;
        private TimeSpan timeout;
        private boolean compressRequest;
        private Keystore keystore;
        @NestedConfigurationProperty
        private Caching caching;

        @Getter
        @Setter
        public static final class Keystore {
            private String path;
            private String password;
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static final class ThreadPool {
        private Integer minSize = 1;
        private Integer maxSize;
        private TimeSpan keepAlive = TimeSpan.of(1, MINUTES);
        private Integer queueSize = 0;

        public ThreadPool(final Integer maxSize) {
            this.maxSize = maxSize;
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static final class OAuth {
        private Boolean enabled = Boolean.FALSE;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static final class Retry {
        private TimeSpan fixedDelay;
        private Backoff backoff;
        private Integer maxRetries;
        private TimeSpan maxDuration;
        private Double jitterFactor;
        private TimeSpan jitter;

        @Getter
        @Setter
        @NoArgsConstructor
        @AllArgsConstructor
        public static final class Backoff {
            private TimeSpan delay;
            private TimeSpan maxDelay;
            private Double delayFactor;
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static final class CircuitBreaker {
        private Ratio failureThreshold;
        private TimeSpan delay = TimeSpan.of(0, SECONDS);
        private Ratio successThreshold;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static final class BackupRequest {
        private TimeSpan delay;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static final class Caching {
        private Boolean shared = Boolean.TRUE;
        private Path directory;
        private Integer maxObjectSize = CacheConfig.DEFAULT_MAX_OBJECT_SIZE_BYTES;
        private Integer maxCacheEntries = CacheConfig.DEFAULT_MAX_CACHE_ENTRIES;
        private Heuristic heuristic;

        @Getter
        @Setter
        @NoArgsConstructor
        @AllArgsConstructor
        public static final class Heuristic {
            private Float coefficient = CacheConfig.DEFAULT_HEURISTIC_COEFFICIENT;
            private TimeSpan defaultLifeTime = TimeSpan.of(CacheConfig.DEFAULT_HEURISTIC_LIFETIME, SECONDS);
        }
    }
}

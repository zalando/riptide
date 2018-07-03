package org.zalando.riptide.spring;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apiguardian.api.API;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.zalando.riptide.UrlResolution;

import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
        private UrlResolution urlResolution;
        private TimeSpan connectTimeout;
        private TimeSpan socketTimeout;
        private TimeSpan connectionTimeToLive;
        private Integer maxConnectionsPerRoute;
        private Integer maxConnectionsTotal;
        @NestedConfigurationProperty
        private ThreadPool threadPool;
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
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static final class GlobalOAuth {
        private URI accessTokenUrl;
        private Path credentialsDirectory;
        private TimeSpan schedulingPeriod = TimeSpan.of(5, SECONDS);
        private TimeSpan connectTimeout = TimeSpan.of(1, SECONDS);
        private TimeSpan socketTimeout = TimeSpan.of(2, SECONDS);
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
        private OAuth oauth;
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
        private boolean compressRequest = false;
        private Keystore keystore;

        @Getter
        @Setter
        @NoArgsConstructor
        @AllArgsConstructor
        public static final class OAuth {
            private List<String> scopes = new ArrayList<>();

        }

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
        private Integer minSize;
        private Integer maxSize;
        private TimeSpan keepAlive;
        private Integer queueSize;
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
        private TimeSpan delay;
        private Ratio successThreshold;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static final class BackupRequest {
        private TimeSpan delay;
    }
}

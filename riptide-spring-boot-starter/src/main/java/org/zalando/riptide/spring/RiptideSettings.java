package org.zalando.riptide.spring;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.lang.System.getenv;
import static java.util.concurrent.TimeUnit.SECONDS;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public final class RiptideSettings {

    private Defaults defaults = new Defaults();
    private GlobalOAuth oauth = new GlobalOAuth();
    private Map<String, Client> clients = new LinkedHashMap<>();

    @Getter
    @Setter
    public static final class Defaults {
        private TimeSpan connectionTimeout = TimeSpan.of(5, SECONDS);
        private TimeSpan socketTimeout = TimeSpan.of(5, SECONDS);
        private TimeSpan connectionTimeToLive = TimeSpan.of(30, SECONDS);
        private int maxConnectionsPerRoute = 2;
        private int maxConnectionsTotal = 20;
        private boolean keepOriginalStackTrace = true;
        private boolean detectTransientFaults = true;
        private Failsafe failsafe;
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
        private TimeSpan connectionTimeout = TimeSpan.of(1, SECONDS);
        private TimeSpan socketTimeout = TimeSpan.of(2, SECONDS);
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static final class Client {
        private String baseUrl;
        private TimeSpan connectionTimeout;
        private TimeSpan socketTimeout;
        private TimeSpan connectionTimeToLive;
        private Integer maxConnectionsPerRoute;
        private Integer maxConnectionsTotal;
        private OAuth oauth;
        private Boolean keepOriginalStackTrace;
        private Boolean detectTransientFaults;
        private Failsafe failsafe;
        private boolean compressRequest = false;
        private Keystore keystore;
        private TimeSpan timeout;

        @Getter
        @Setter
        public static final class OAuth {
            private final List<String> scopes = new ArrayList<>();

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
    public static final class Failsafe {
        private Retry retry;
        private CircuitBreaker circuitBreaker;

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
    }

}

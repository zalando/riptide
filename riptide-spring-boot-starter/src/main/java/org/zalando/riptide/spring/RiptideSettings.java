package org.zalando.riptide.spring;

import lombok.Getter;
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
@Setter
public final class RiptideSettings {

    private final Defaults defaults = new Defaults();
    private final GlobalOAuth oauth = new GlobalOAuth();
    private final Map<String, Client> clients = new LinkedHashMap<>();

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
    }

    @Getter
    @Setter
    public static final class GlobalOAuth {
        private URI accessTokenUrl = Optional.ofNullable(getenv("ACCESS_TOKEN_URL")).map(URI::create).orElse(null);
        private Path credentialsDirectory;
        private TimeSpan schedulingPeriod = TimeSpan.of(5, SECONDS);
        private TimeSpan connectionTimeout = TimeSpan.of(1, SECONDS);
        private TimeSpan socketTimeout = TimeSpan.of(2, SECONDS);
    }

    @Getter
    @Setter
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
        private boolean compressRequest = false;
        private Keystore keystore;
    }

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

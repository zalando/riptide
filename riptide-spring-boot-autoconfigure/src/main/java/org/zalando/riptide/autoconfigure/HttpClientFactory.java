package org.zalando.riptide.autoconfigure;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.client.cache.HttpCacheStorage;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.cache.CacheConfig;
import org.apache.http.impl.client.cache.CachingHttpClientBuilder;
import org.apache.http.impl.client.cache.CachingHttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContexts;
import org.zalando.riptide.autoconfigure.RiptideProperties.Caching;
import org.zalando.riptide.autoconfigure.RiptideProperties.Caching.Heuristic;
import org.zalando.riptide.autoconfigure.RiptideProperties.CertificatePinning;
import org.zalando.riptide.autoconfigure.RiptideProperties.CertificatePinning.Keystore;
import org.zalando.riptide.autoconfigure.RiptideProperties.Client;
import org.zalando.riptide.autoconfigure.RiptideProperties.Connections;

import javax.annotation.Nullable;
import javax.net.ssl.SSLContext;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

@SuppressWarnings("unused")
@Slf4j
final class HttpClientFactory {

    private HttpClientFactory() {

    }

    public static HttpClientConnectionManager createHttpClientConnectionManager(final Client client)
            throws GeneralSecurityException, IOException {

        final Connections connections = client.getConnections();

        final PoolingHttpClientConnectionManager manager = new PoolingHttpClientConnectionManager(
                RegistryBuilder.<ConnectionSocketFactory>create()
                        .register("http", PlainConnectionSocketFactory.getSocketFactory())
                        .register("https", new SSLConnectionSocketFactory(createSSLContext(client)))
                        .build(),
                null, // connection factory
                null, // scheme port resolver
                null, // dns resolver
                connections.getTimeToLive().getAmount(),
                connections.getTimeToLive().getUnit());

        manager.setMaxTotal(connections.getMaxTotal());
        manager.setDefaultMaxPerRoute(connections.getMaxPerRoute());

        return manager;
    }

    public static CloseableHttpClient createHttpClient(final Client client,
            final List<HttpRequestInterceptor> firstRequestInterceptors,
            final List<HttpRequestInterceptor> lastRequestInterceptors,
            final List<HttpResponseInterceptor> lastResponseInterceptors,
            final HttpClientConnectionManager connectionManager,
            @Nullable final HttpClientCustomizer customizer,
            @Nullable final HttpCacheStorage cacheStorage) {

        final Caching caching = client.getCaching();
        final HttpClientBuilder builder = caching.getEnabled() ?
                configureCaching(caching, cacheStorage) :
                HttpClientBuilder.create();

        final RequestConfig.Builder config = RequestConfig.custom();

        firstRequestInterceptors.forEach(builder::addInterceptorFirst);
        lastRequestInterceptors.forEach(builder::addInterceptorLast);
        lastResponseInterceptors.forEach(builder::addInterceptorLast);

        final Connections connections = client.getConnections();
        config.setConnectTimeout((int) connections.getConnectTimeout().to(MILLISECONDS));
        config.setSocketTimeout((int) connections.getSocketTimeout().to(MILLISECONDS));

        builder.setConnectionManager(connectionManager);
        builder.setDefaultRequestConfig(config.build());
        builder.disableAutomaticRetries();

        Optional.ofNullable(customizer).ifPresent(customize(builder));

        return builder.build();
    }

    private static CachingHttpClientBuilder configureCaching(final Caching caching,
            @Nullable final HttpCacheStorage cacheStorage) {
        final Heuristic heuristic = caching.getHeuristic();

        final CacheConfig.Builder config = CacheConfig.custom()
                .setSharedCache(caching.getShared())
                .setMaxObjectSize(caching.getMaxObjectSize())
                .setMaxCacheEntries(caching.getMaxCacheEntries());

        if (heuristic.getEnabled()) {
            config.setHeuristicCachingEnabled(true);
            config.setHeuristicCoefficient(heuristic.getCoefficient());
            config.setHeuristicDefaultLifetime(heuristic.getDefaultLifeTime().to(TimeUnit.SECONDS));
        }

        return CachingHttpClients.custom()
                .setCacheConfig(config.build())
                .setHttpCacheStorage(cacheStorage)
                .setCacheDir(Optional.ofNullable(caching.getDirectory())
                        .map(Path::toFile)
                        .orElse(null));
    }

    private static SSLContext createSSLContext(final Client client) throws GeneralSecurityException, IOException {
        final CertificatePinning pinning = client.getCertificatePinning();

        if (pinning.getEnabled()) {
            final Keystore keystore = pinning.getKeystore();
            final String path = keystore.getPath();
            final String password = keystore.getPassword();

            final URL resource = HttpClientFactory.class.getClassLoader().getResource(path);

            if (resource == null) {
                throw new FileNotFoundException(format("Keystore [%s] not found.", path));
            }

            try {
                return SSLContexts.custom()
                        .loadTrustMaterial(resource, password == null ? null : password.toCharArray())
                        .build();
            } catch (final Exception e) {
                log.error("Error loading keystore [{}]:", path,
                        e); // log full exception, bean initialization code swallows it
                throw e;
            }
        }

        return SSLContexts.createDefault();
    }

    private static Consumer<HttpClientCustomizer> customize(final HttpClientBuilder builder) {
        return customizer -> customizer.customize(builder);
    }

}

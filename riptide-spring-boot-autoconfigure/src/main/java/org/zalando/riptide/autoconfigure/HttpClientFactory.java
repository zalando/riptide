package org.zalando.riptide.autoconfigure;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContexts;
import org.zalando.riptide.autoconfigure.RiptideProperties.Client;

import javax.annotation.Nullable;
import javax.net.ssl.SSLContext;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static java.lang.String.format;

@SuppressWarnings("unused")
@Slf4j
final class HttpClientFactory {

    private HttpClientFactory() {

    }

    public static HttpClientConnectionManager createHttpClientConnectionManager(final Client client)
            throws GeneralSecurityException, IOException {

        final PoolingHttpClientConnectionManager manager = new PoolingHttpClientConnectionManager(
                RegistryBuilder.<ConnectionSocketFactory>create()
                        .register("http", PlainConnectionSocketFactory.getSocketFactory())
                        .register("https", new SSLConnectionSocketFactory(createSSLContext(client)))
                        .build(),
                null, // connection factory
                null, // scheme port resolver
                null, // dns resolver
                client.getConnectionTimeToLive().getAmount(),
                client.getConnectionTimeToLive().getUnit());

        manager.setMaxTotal(client.getMaxConnectionsTotal());
        manager.setDefaultMaxPerRoute(client.getMaxConnectionsPerRoute());

        return manager;
    }

    public static CloseableHttpClient createHttpClient(final Client client,
            final List<HttpRequestInterceptor> firstRequestInterceptors,
            final List<HttpRequestInterceptor> lastRequestInterceptors,
            final List<HttpResponseInterceptor> lastResponseInterceptors,
            final HttpClientConnectionManager connectionManager,
            @Nullable final HttpClientCustomizer customizer) {

        final HttpClientBuilder builder = HttpClientBuilder.create();
        final RequestConfig.Builder config = RequestConfig.custom();

        firstRequestInterceptors.forEach(builder::addInterceptorFirst);
        lastRequestInterceptors.forEach(builder::addInterceptorLast);
        lastResponseInterceptors.forEach(builder::addInterceptorLast);

        config.setConnectTimeout((int) client.getConnectTimeout().to(TimeUnit.MILLISECONDS));
        config.setSocketTimeout((int) client.getSocketTimeout().to(TimeUnit.MILLISECONDS));

        builder.setConnectionManager(connectionManager);

        builder.setDefaultRequestConfig(config.build());
        Optional.ofNullable(customizer).ifPresent(customize(builder));

        return builder.build();
    }

    private static SSLContext createSSLContext(final Client client) throws GeneralSecurityException, IOException {
        @Nullable final Client.Keystore keystore = client.getKeystore();

        if (keystore == null) {
            return SSLContexts.createDefault();
        }

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

    private static Consumer<HttpClientCustomizer> customize(final HttpClientBuilder builder) {
        return customizer -> customizer.customize(builder);
    }

}

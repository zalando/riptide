package org.zalando.riptide.spring;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.zalando.riptide.spring.RiptideProperties.Client;

import javax.annotation.Nullable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static java.lang.String.format;
import static org.apache.http.conn.ssl.SSLConnectionSocketFactory.getDefaultHostnameVerifier;

@SuppressWarnings("unused")
@Slf4j
final class HttpClientFactory {

    private HttpClientFactory() {

    }

    public static CloseableHttpClient createHttpClient(final Client client,
            final List<HttpRequestInterceptor> firstRequestInterceptors,
            final List<HttpRequestInterceptor> lastRequestInterceptors,
            final List<HttpResponseInterceptor> lastResponseInterceptors,
            @Nullable final HttpClientCustomizer customizer) throws GeneralSecurityException, IOException {

        final HttpClientBuilder builder = HttpClientBuilder.create();
        final RequestConfig.Builder config = RequestConfig.custom();

        firstRequestInterceptors.forEach(builder::addInterceptorFirst);
        lastRequestInterceptors.forEach(builder::addInterceptorLast);
        lastResponseInterceptors.forEach(builder::addInterceptorLast);

        config.setConnectTimeout((int) client.getConnectTimeout().to(TimeUnit.MILLISECONDS));
        config.setSocketTimeout((int) client.getSocketTimeout().to(TimeUnit.MILLISECONDS));
        builder.setConnectionTimeToLive(
                client.getConnectionTimeToLive().getAmount(),
                client.getConnectionTimeToLive().getUnit());
        builder.setMaxConnPerRoute(client.getMaxConnectionsPerRoute());
        builder.setMaxConnTotal(client.getMaxConnectionsTotal());

        if (client.getKeystore() != null) {
            builder.setSSLSocketFactory(createSSLConnectionFactory(client));
        }

        builder.setDefaultRequestConfig(config.build());
        Optional.ofNullable(customizer).ifPresent(customize(builder));

        return builder.build();
    }

    private static SSLConnectionSocketFactory createSSLConnectionFactory(final Client client)
            throws GeneralSecurityException, IOException {
        final Client.Keystore keystore = client.getKeystore();

        final SSLContextBuilder ssl = SSLContexts.custom();

        final String path = keystore.getPath();
        final String password = keystore.getPassword();

        final URL resource = HttpClientFactory.class.getClassLoader().getResource(path);

        if (resource == null) {
            throw new FileNotFoundException(format("Keystore [%s] not found.", path));
        }

        try {
            ssl.loadTrustMaterial(resource, password == null ? null : password.toCharArray());
            return new SSLConnectionSocketFactory(ssl.build(), getDefaultHostnameVerifier());
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

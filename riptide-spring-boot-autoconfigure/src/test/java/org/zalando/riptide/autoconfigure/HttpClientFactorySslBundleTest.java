package org.zalando.riptide.autoconfigure;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.zalando.riptide.autoconfigure.HttpClientFactory.createHttpClientConnectionManagerWithSslBundle;
import static org.zalando.riptide.autoconfigure.HttpClientFactory.createSslContextFromSslBundle;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import javax.net.ssl.SSLContext;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.support.SimpleBeanDefinitionRegistry;
import org.springframework.boot.ssl.DefaultSslBundleRegistry;
import org.springframework.boot.ssl.NoSuchSslBundleException;
import org.springframework.boot.ssl.SslBundle;
import org.zalando.riptide.autoconfigure.RiptideProperties.CertificatePinning;
import org.zalando.riptide.autoconfigure.RiptideProperties.Defaults;
import org.zalando.riptide.autoconfigure.RiptideProperties.SslBundleUsage;
import org.zalando.riptide.autoconfigure.RiptideProperties.Threads;

public class HttpClientFactorySslBundleTest {

  @Test
  void shouldFailWithoutSslBundle() {
    final RiptideProperties.Client client = new RiptideProperties.Client();
    client.setSslBundleUsage(new SslBundleUsage(true, null));

    final NoSuchSslBundleException exception = assertThrows(
        NoSuchSslBundleException.class,
        () -> createHttpClientConnectionManagerWithSslBundle(
            withDefaults(client), "example", new DefaultSslBundleRegistry()
        )
    );

    assertThat(exception.getMessage(), containsString("'example'"));
  }

  @Test
  void shouldFailWithSslBundle() {
    final RiptideProperties.Client client = new RiptideProperties.Client();
    client.setSslBundleUsage(new SslBundleUsage(true, "configured-bundle-id"));

    final NoSuchSslBundleException exception = assertThrows(
        NoSuchSslBundleException.class,
        () -> createHttpClientConnectionManagerWithSslBundle(
            withDefaults(client), "example", new DefaultSslBundleRegistry()
        )
    );

    assertThat(exception.getMessage(), containsString("'configured-bundle-id'"));
  }

  @Test
  void shouldCreateMockedSslContext() {
    SslBundle sslBundle = Mockito.mock(SslBundle.class);
    Mockito.when(sslBundle.createSslContext()).thenReturn(Mockito.mock(SSLContext.class));
    final RiptideProperties.Client client = new RiptideProperties.Client();
    client.setSslBundleUsage(new SslBundleUsage(true, "configured-bundle-id"));

    DefaultSslBundleRegistry registry = new DefaultSslBundleRegistry();
    registry.registerBundle("configured-bundle-id", sslBundle);

    SSLContext result = createSslContextFromSslBundle(withDefaults(client), "example",
        registry);

    assertNotNull(result);
    verify(sslBundle, Mockito.times(1)).createSslContext();
  }

  @Test
  void shouldCreateDefaultSslContext() {
    final RiptideProperties.Client client = new RiptideProperties.Client();
    client.setSslBundleUsage(new SslBundleUsage(false, null));

    SSLContext result = createSslContextFromSslBundle(
        withDefaults(client), "example", new DefaultSslBundleRegistry()
    );

    assertNotNull(result);
  }

  @Test
  void shouldCreateConnectionManagerWithDefaultSslContext() {
    final RiptideProperties.Client client = new RiptideProperties.Client();
    client.setSslBundleUsage(new SslBundleUsage(false, null));

    DefaultSslBundleRegistry registry = new DefaultSslBundleRegistry();

    HttpClientConnectionManager result = createHttpClientConnectionManagerWithSslBundle(
        withDefaults(client), "example",registry
    );

    assertNotNull(result);
  }

  @Test
  void shouldFailWhenCertificatePinningAndSslBundleUsageAreConfigured() {
    final RiptideProperties props = new RiptideProperties();
    final RiptideProperties.Client client = new RiptideProperties.Client();

    client.setThreads(new Threads(false, 0,0, null, 0));
    client.setSslBundleUsage(new SslBundleUsage(true, null));
    client.setCertificatePinning(new CertificatePinning(true, null));

    props.setClients(Map.of("ssl-bundle-test", client));

    DefaultRiptideRegistrar registrar = new DefaultRiptideRegistrar(
        new Registry(new SimpleBeanDefinitionRegistry()), props
    );

    final SslBundleUsageOrCertificatePinningException ex = assertThrows(
        SslBundleUsageOrCertificatePinningException.class, () -> registrar.register()
    );

    assertEquals(
        ex.getMessage(),
        "CertificatePinning and SslBundleUsage configured at same time in http-client : 'ssl-bundle-test'"
    );
  }

  private RiptideProperties.Client withDefaults(final RiptideProperties.Client client) {
    final RiptideProperties properties = Defaulting.withDefaults(
        new RiptideProperties(new Defaults(), ImmutableMap.of("example", client)));

    return properties.getClients().get("example");
  }

}

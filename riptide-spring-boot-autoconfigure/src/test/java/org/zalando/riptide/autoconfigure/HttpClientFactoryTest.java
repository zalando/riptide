package org.zalando.riptide.autoconfigure;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;
import org.zalando.riptide.autoconfigure.RiptideProperties.Client.Keystore;
import org.zalando.riptide.autoconfigure.RiptideProperties.Defaults;
import org.zalando.riptide.autoconfigure.RiptideProperties.GlobalOAuth;

import java.io.FileNotFoundException;
import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class HttpClientFactoryTest {

    @Test
    void shouldFailOnKeystoreNotFound() throws Exception {
        final Keystore nonExistingKeystore = new Keystore();
        nonExistingKeystore.setPath("i-do-not-exist.keystore");

        final RiptideProperties.Client client = new RiptideProperties.Client();
        client.setKeystore(nonExistingKeystore);

        final FileNotFoundException exception = assertThrows(FileNotFoundException.class, () ->
                HttpClientFactory.createHttpClientConnectionManager(withDefaults(client)));

        assertThat(exception.getMessage(), containsString("i-do-not-exist.keystore"));
    }

    @Test
    void shouldFailOnInvalidKeystore() throws Exception {
        final Keystore invalidKeystore = new Keystore();
        invalidKeystore.setPath("application-default.yml");

        final RiptideProperties.Client client = new RiptideProperties.Client();
        client.setKeystore(invalidKeystore);

        assertThrows(IOException.class, () ->
        HttpClientFactory.createHttpClientConnectionManager(withDefaults(client)));
    }

    private RiptideProperties.Client withDefaults(final RiptideProperties.Client client) {
        final RiptideProperties properties = Defaulting.withDefaults(
                new RiptideProperties(new Defaults(), new GlobalOAuth(), ImmutableMap.of("example", client)));

        return properties.getClients().get("example");
    }

}

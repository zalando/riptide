package org.zalando.riptide.spring;

import com.google.common.collect.ImmutableMap;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.zalando.riptide.spring.RiptideProperties.Client.Keystore;
import org.zalando.riptide.spring.RiptideProperties.Defaults;
import org.zalando.riptide.spring.RiptideProperties.GlobalOAuth;

import java.io.FileNotFoundException;
import java.io.IOException;

import static java.util.Collections.emptyList;

public class HttpClientFactoryTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void shouldFailOnKeystoreNotFound() throws Exception {
        exception.expect(FileNotFoundException.class);
        exception.expectMessage("i-do-not-exist.keystore");

        final Keystore nonExistingKeystore = new Keystore();
        nonExistingKeystore.setPath("i-do-not-exist.keystore");

        final RiptideProperties.Client client = new RiptideProperties.Client();
        client.setKeystore(nonExistingKeystore);

        HttpClientFactory.createHttpClient(withDefaults(client), emptyList(), emptyList(),emptyList(), null);
    }

    @Test
    public void shouldFailOnInvalidKeystore() throws Exception {
        exception.expect(IOException.class);
        exception.expectMessage("Invalid keystore format");

        final Keystore invalidKeystore = new Keystore();
        invalidKeystore.setPath("application-default.yml");

        final RiptideProperties.Client client = new RiptideProperties.Client();
        client.setKeystore(invalidKeystore);

        HttpClientFactory.createHttpClient(withDefaults(client), emptyList(), emptyList(),emptyList(), null);
    }

    private RiptideProperties.Client withDefaults(final RiptideProperties.Client client) {
        final RiptideProperties properties = Defaulting.withDefaults(
                new RiptideProperties(new Defaults(), new GlobalOAuth(), ImmutableMap.of("example", client)));

        return properties.getClients().get("example");
    }

}

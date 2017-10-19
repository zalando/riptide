package org.zalando.riptide.spring;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.zalando.riptide.spring.RiptideSettings.Client.Keystore;

import java.io.FileNotFoundException;
import java.io.IOException;

public class HttpClientFactoryBeanTest {

    private HttpClientFactoryBean unit = new HttpClientFactoryBean();

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void shouldFailOnKeystoreNotFound() throws Exception {
        exception.expect(FileNotFoundException.class);
        exception.expectMessage("i-do-not-exist.keystore");

        final Keystore nonExistingKeystore = new Keystore();
        nonExistingKeystore.setPath("i-do-not-exist.keystore");
        unit.setTrustedKeystore(nonExistingKeystore);
    }

    @Test
    public void shouldFailOnInvalidKeystore() throws Exception {
        exception.expect(IOException.class);
        exception.expectMessage("Invalid keystore format");

        final Keystore invalidKeystore = new Keystore();
        invalidKeystore.setPath("application-default.yml");
        unit.setTrustedKeystore(invalidKeystore);
    }

    // just because spring sometimes fails to destroy properly during tests
    @Test
    public void shouldDestroy() throws Exception {
        unit.afterPropertiesSet();
        unit.getObject();
        unit.destroy();;
    }

}

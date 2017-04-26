package org.zalando.riptide;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.net.URI;

public class NoBaseUrlTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void shouldFailOnNonAbsoluteBaseUrl() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Base URL is not absolute");

        Rest.builder().baseUrl("");
    }

    @Test
    public void shouldFailOnNonAbsoluteBaseUri() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Base URL is not absolute");

        Rest.builder().baseUrl(URI.create(""));
    }

    @Test
    public void shouldFailOnProvisioningOfNonAbsoluteBaseUri() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Base URL is not absolute");

        final Rest unit = Rest.builder()
                .baseUrl(() -> URI.create(""))
                .requestFactory(new SimpleClientHttpRequestFactory())
                .build();

        unit.get();
    }

}

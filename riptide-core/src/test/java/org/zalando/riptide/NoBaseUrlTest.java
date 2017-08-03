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

        Http.builder().baseUrl("");
    }

    @Test
    public void shouldFailOnNonAbsoluteBaseUri() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Base URL is not absolute");

        Http.builder().baseUrl(URI.create(""));
    }

    @Test
    public void shouldFailOnProvisioningOfNonAbsoluteBaseUri() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Base URL is not absolute");

        final Http unit = Http.builder()
                .baseUrl(() -> URI.create(""))
                .requestFactory(new SimpleClientHttpRequestFactory())
                .build();

        unit.get();
    }

}

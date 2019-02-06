package org.zalando.riptide;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.net.URI;

public class NoBaseUrlTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    private final ClientHttpRequestFactory requestFactory = Mockito.mock(ClientHttpRequestFactory.class);

    @Test
    public void shouldFailOnNonAbsoluteBaseUrl() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Base URL is not absolute");

        Http.builder().executor(Runnable::run).requestFactory(requestFactory).baseUrl("");
    }

    @Test
    public void shouldFailOnNonAbsoluteBaseUri() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Base URL is not absolute");

        Http.builder().executor(Runnable::run).requestFactory(requestFactory).baseUrl(URI.create(""));
    }

    @Test
    public void shouldFailOnProvisioningOfNonAbsoluteBaseUri() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Base URL is not absolute");

        final Http unit = Http.builder()
                .executor(Runnable::run)
                .requestFactory(new SimpleClientHttpRequestFactory())
                .baseUrl(() -> URI.create(""))
                .build();

        unit.get();
    }

}

package org.zalando.riptide;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.net.URI;
import java.util.concurrent.ExecutorService;

public class NoBaseUrlTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    private final ExecutorService executor = Mockito.mock(ExecutorService.class);

    @Test
    public void shouldFailOnNonAbsoluteBaseUrl() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Base URL is not absolute");

        Http.builder().simpleRequestFactory(executor).baseUrl("");
    }

    @Test
    public void shouldFailOnNonAbsoluteBaseUri() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Base URL is not absolute");

        Http.builder().simpleRequestFactory(executor).baseUrl(URI.create(""));
    }

    @Test
    public void shouldFailOnProvisioningOfNonAbsoluteBaseUri() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Base URL is not absolute");

        final Http unit = Http.builder()
                .requestFactory(new SimpleClientHttpRequestFactory())
                .baseUrl(() -> URI.create(""))
                .build();

        unit.get();
    }

}

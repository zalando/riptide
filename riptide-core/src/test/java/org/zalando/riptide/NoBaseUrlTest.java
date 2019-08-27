package org.zalando.riptide;

import org.junit.jupiter.api.*;
import org.mockito.*;
import org.springframework.http.client.*;

import java.net.*;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

final class NoBaseUrlTest {

    private final ClientHttpRequestFactory requestFactory = Mockito.mock(ClientHttpRequestFactory.class);

    @Test
    void shouldFailOnNonAbsoluteBaseUrl() {
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                Http.builder().executor(Runnable::run).requestFactory(requestFactory).baseUrl(""));

        assertThat(exception.getMessage(), containsString("Base URL is not absolute"));
    }

    @Test
    void shouldFailOnNonAbsoluteBaseUri() {
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
        Http.builder().executor(Runnable::run).requestFactory(requestFactory).baseUrl(URI.create("")));

        assertThat(exception.getMessage(), containsString("Base URL is not absolute"));
    }

    @Test
    void shouldFailOnProvisioningOfNonAbsoluteBaseUri() {
        final Http unit = Http.builder()
                .executor(Runnable::run)
                .requestFactory(new SimpleClientHttpRequestFactory())
                .baseUrl(() -> URI.create(""))
                .build();

        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, unit::get);

        assertThat(exception.getMessage(), containsString("Base URL is not absolute"));
    }

}

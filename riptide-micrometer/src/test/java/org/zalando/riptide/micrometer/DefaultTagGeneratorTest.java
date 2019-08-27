package org.zalando.riptide.micrometer;

import io.micrometer.core.instrument.Tag;
import org.junit.jupiter.api.*;
import org.springframework.http.*;
import org.springframework.http.client.*;
import org.zalando.riptide.*;

import java.io.*;
import java.net.*;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

final class DefaultTagGeneratorTest {

    private final TagGenerator unit = new DefaultTagGenerator();

    private final RequestArguments arguments = RequestArguments.create()
            .withMethod(HttpMethod.GET)
            .withBaseUrl(URI.create("http://localhost/"));

    private final ClientHttpResponse response = mock(ClientHttpResponse.class);

    @Test
    void shouldGenerateRequestTags() throws IOException {
        when(response.getRawStatusCode()).thenReturn(200);

        final Iterable<Tag> tags = unit.tags(arguments, response, null);

        assertThat(tags, hasItems(Tag.of("method", "GET"), Tag.of("uri", "/"), Tag.of("clientName", "localhost")));
    }

    @Test
    void shouldGenerateResponseTags() throws IOException {
        when(response.getRawStatusCode()).thenReturn(200);

        final Iterable<Tag> tags = unit.tags(arguments, response, null);

        assertThat(tags, hasItems(Tag.of("status", "200"), Tag.of("exception", "None")));
    }

    @Test
    void shouldGenerateResponseTagsOnTimeout() {
        final Iterable<Tag> tags = unit.tags(arguments, null, new SocketTimeoutException());

        assertThat(tags, hasItems(Tag.of("status", "CLIENT_ERROR"), Tag.of("exception", "SocketTimeoutException")));
    }

    @Test
    void shouldGenerateResponseTagsOnIOException() throws IOException {
        when(response.getRawStatusCode()).thenThrow(IOException.class);

        final Iterable<Tag> tags = unit.tags(arguments, response, null);

        assertThat(tags, hasItems(Tag.of("status", "IO_ERROR")));
    }


}

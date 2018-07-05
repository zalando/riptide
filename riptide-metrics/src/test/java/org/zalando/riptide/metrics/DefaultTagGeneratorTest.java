package org.zalando.riptide.metrics;

import io.micrometer.core.instrument.Tag;
import org.junit.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpResponse;
import org.zalando.riptide.RequestArguments;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URI;

import static org.hamcrest.Matchers.hasItems;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public final class DefaultTagGeneratorTest {

    private final TagGenerator unit = new DefaultTagGenerator();

    private final RequestArguments arguments = RequestArguments.create()
            .withMethod(HttpMethod.GET)
            .withRequestUri(URI.create("http://localhost/"));

    private final ClientHttpResponse response = mock(ClientHttpResponse.class);

    @Test
    public void shouldGenerateRequestTags() throws IOException {
        when(response.getRawStatusCode()).thenReturn(200);

        final Iterable<Tag> tags = unit.tags(arguments, response, null);

        assertThat(tags, hasItems(Tag.of("method", "GET"), Tag.of("uri", "/"), Tag.of("clientName", "localhost")));
    }

    @Test
    public void shouldGenerateResponseTags() throws IOException {
        when(response.getRawStatusCode()).thenReturn(200);

        final Iterable<Tag> tags = unit.tags(arguments, response, null);

        assertThat(tags, hasItems(Tag.of("status", "200"), Tag.of("exception", "None")));
    }

    @Test
    public void shouldGenerateResponseTagsOnTimeout() {
        final Iterable<Tag> tags = unit.tags(arguments, null, new SocketTimeoutException());

        assertThat(tags, hasItems(Tag.of("status", "CLIENT_ERROR"), Tag.of("exception", "SocketTimeoutException")));
    }

    @Test
    public void shouldGenerateResponseTagsOnIOException() throws IOException {
        when(response.getRawStatusCode()).thenThrow(IOException.class);

        final Iterable<Tag> tags = unit.tags(arguments, response, null);

        assertThat(tags, hasItems(Tag.of("status", "IO_ERROR")));
    }


}

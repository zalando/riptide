package org.zalando.riptide.httpclient;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.core5.http.HttpEntity;
import org.junit.jupiter.api.Test;
import org.springframework.http.StreamingHttpOutputMessage.Body;
import org.springframework.http.client.ClientHttpRequest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

final class StreamingApacheClientHttpRequestTest {

    @Test
    void shouldUseStreamingEntity() {
        final HttpClient client = mock(HttpClient.class);
        final HttpPost request = new HttpPost("https://example.org");

        final StreamingApacheClientHttpRequest unit = new StreamingApacheClientHttpRequest(client, request);

        unit.setBody(mock(Body.class));

        final HttpEntity entity = request.getEntity();

        assertFalse(entity.isStreaming());
        assertThrows(UnsupportedOperationException.class, entity::getContent);
    }

    @Test
    void shouldNotSupportGetBody() {
        final HttpClient client = mock(HttpClient.class);
        final HttpPost request = new HttpPost("https://example.org");

        final ClientHttpRequest unit = new StreamingApacheClientHttpRequest(client, request);

        assertThrows(UnsupportedOperationException.class, unit::getBody);
    }

}

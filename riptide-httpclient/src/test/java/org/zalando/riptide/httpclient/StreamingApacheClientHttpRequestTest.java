package org.zalando.riptide.httpclient;

import org.apache.http.HttpEntity;
import org.apache.http.client.*;
import org.apache.http.client.methods.*;
import org.junit.jupiter.api.*;
import org.springframework.http.*;
import org.springframework.http.StreamingHttpOutputMessage.*;
import org.springframework.http.client.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

final class StreamingApacheClientHttpRequestTest {

    @Test
    void shouldUseStreamingEntity() {
        final HttpClient client = mock(HttpClient.class);
        final HttpPost request = new HttpPost();

        final StreamingApacheClientHttpRequest unit = new StreamingApacheClientHttpRequest(client, request);

        unit.setBody(mock(Body.class));

        final HttpEntity entity = request.getEntity();

        assertFalse(entity.isStreaming());
        assertThrows(UnsupportedOperationException.class, entity::getContent);
        assertThrows(UnsupportedOperationException.class, entity::consumeContent);
    }

    @Test
    void shouldNotSupportGetBody() {
        final HttpClient client = mock(HttpClient.class);
        final HttpPost request = new HttpPost();

        final ClientHttpRequest unit = new StreamingApacheClientHttpRequest(client, request);

        assertThrows(UnsupportedOperationException.class, unit::getBody);
    }

    @Test
    void shouldFailOnNonBodyRequests() {
        final HttpClient client = mock(HttpClient.class);

        final StreamingHttpOutputMessage unit = new StreamingApacheClientHttpRequest(client, new HttpDelete());

        assertThrows(IllegalStateException.class, () -> unit.setBody(mock(Body.class)));
    }
    
}

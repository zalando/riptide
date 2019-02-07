package org.zalando.riptide.httpclient;

import org.apache.http.conn.ConnectionReleaseTrigger;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.io.InputStream;

import static org.zalando.fauxpas.FauxPas.throwingRunnable;

final class ApacheClientHttpResponse implements ClientHttpResponse {

    private final ClientHttpResponse response;

    ApacheClientHttpResponse(final ClientHttpResponse response) {
        this.response = response;
    }

    @Override
    public HttpStatus getStatusCode() throws IOException {
        return response.getStatusCode();
    }

    @Override
    public int getRawStatusCode() throws IOException {
        return response.getRawStatusCode();
    }

    @Override
    public String getStatusText() throws IOException {
        return response.getStatusText();
    }

    @Override
    public InputStream getBody() throws IOException {
        return new EndOfStreamDetectingInputStream(response.getBody(), (body, ended) -> {
            if (body instanceof ConnectionReleaseTrigger) {
                // effectively releasing the connection back to the pool in order to prevent starvation
                final ConnectionReleaseTrigger trigger = (ConnectionReleaseTrigger) body;

                if (ended) {
                    // Stream was fully consumed, connection can therefore be reused.
                    trigger.releaseConnection();
                } else {
                    // Stream was not fully consumed, connection needs to be discarded.
                    // We can't just consume the remaining bytes since the stream could be endless.
                    trigger.abortConnection();
                }
            }
            body.close();
        });
    }

    @Override
    public HttpHeaders getHeaders() {
        return response.getHeaders();
    }

    @Override
    public void close() {
        // no clue why ClientHttpResponse#close doesn't allow IOExceptions to be thrown...
        throwingRunnable(() -> getBody().close()).run();
        response.close();
    }

}

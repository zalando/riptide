package org.zalando.riptide.failsafe;

import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

final class RetryException extends RuntimeException {

    private final ClientHttpResponse response;

    RetryException(final ClientHttpResponse response) throws IOException {
        super(formatMessage(response));
        this.response = response;
    }

    private static String formatMessage(final ClientHttpResponse response) throws IOException {
        return String.format("Giving up retrying response: %d - %s\n%s",
                response.getRawStatusCode(), response.getStatusText(), response.getHeaders());
    }
}

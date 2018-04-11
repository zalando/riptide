package org.zalando.riptide.failsafe;

import org.springframework.http.client.ClientHttpResponse;
import org.zalando.riptide.HttpResponseException;

import java.io.IOException;

public final class RetryException extends HttpResponseException {

    RetryException(final ClientHttpResponse response) throws IOException {
        super("Retrying response", response);
    }

}

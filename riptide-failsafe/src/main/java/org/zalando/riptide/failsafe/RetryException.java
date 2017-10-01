package org.zalando.riptide.failsafe;

import org.springframework.http.client.ClientHttpResponse;
import org.zalando.riptide.BaseException;

import java.io.IOException;

final class RetryException extends BaseException {

    RetryException(final ClientHttpResponse response) throws IOException {
        super("Retrying response", response);
    }

}

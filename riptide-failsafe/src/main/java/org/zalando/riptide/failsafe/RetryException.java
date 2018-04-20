package org.zalando.riptide.failsafe;

import org.apiguardian.api.API;
import org.springframework.http.client.ClientHttpResponse;
import org.zalando.riptide.HttpResponseException;

import java.io.IOException;

import static org.apiguardian.api.API.Status.STABLE;

@API(status = STABLE)
public final class RetryException extends HttpResponseException {

    RetryException(final ClientHttpResponse response) throws IOException {
        super("Retrying response", response);
    }

}

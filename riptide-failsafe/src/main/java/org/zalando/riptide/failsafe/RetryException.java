package org.zalando.riptide.failsafe;

import org.apiguardian.api.*;
import org.springframework.http.client.*;
import org.zalando.riptide.*;

import java.io.*;

import static org.apiguardian.api.API.Status.*;

@API(status = STABLE)
public final class RetryException extends HttpResponseException {

    RetryException(final ClientHttpResponse response) throws IOException {
        super("Retrying response", response);
    }

}

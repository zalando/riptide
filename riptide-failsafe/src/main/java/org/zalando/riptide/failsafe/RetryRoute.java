package org.zalando.riptide.failsafe;

import org.apiguardian.api.*;
import org.springframework.http.client.*;
import org.zalando.riptide.*;

import java.io.*;

import static org.apiguardian.api.API.Status.*;

@API(status = STABLE)
public final class RetryRoute implements Route {

    private static final Route RETRY = new RetryRoute();

    private RetryRoute() {

    }

    @Override
    public void execute(final ClientHttpResponse response, final MessageReader reader) throws IOException {
        throw new RetryException(response);
    }

    public static Route retry() {
        return RETRY;
    }

}

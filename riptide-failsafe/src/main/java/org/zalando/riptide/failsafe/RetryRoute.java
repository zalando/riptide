package org.zalando.riptide.failsafe;

import org.springframework.http.client.ClientHttpResponse;
import org.zalando.riptide.MessageReader;
import org.zalando.riptide.Route;

import java.io.IOException;

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

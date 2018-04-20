package org.zalando.riptide.failsafe;

import org.apiguardian.api.API;
import org.springframework.http.client.ClientHttpResponse;
import org.zalando.riptide.MessageReader;
import org.zalando.riptide.Route;

import java.io.IOException;

import static org.apiguardian.api.API.Status.STABLE;

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

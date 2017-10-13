package org.zalando.riptide.failsafe;

import org.springframework.http.client.ClientHttpResponse;
import org.zalando.riptide.MessageReader;
import org.zalando.riptide.Route;

public final class RetryRoute {

    RetryRoute() {
        // package private so we can trick code coverage
    }

    public static Route retry() {
        return Impl.RETRY;
    }

    private enum Impl implements Route {
        RETRY;

        @Override
        public void execute(final ClientHttpResponse response, final MessageReader reader) throws Exception {
            throw new RetryException(response);
        }
    }

}

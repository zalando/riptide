package org.zalando.riptide;

import org.springframework.http.client.ClientHttpResponse;

public final class NoRoute {

    NoRoute() {
        // package private so we can trick code coverage
    }

    public static Route noRoute() {
        return Impl.NO_ROUTE;
    }

    private enum Impl implements Route {
        NO_ROUTE;

        @Override
        public void execute(final ClientHttpResponse response, final MessageReader reader) throws Exception {
            throw new NoRouteException(response);
        }
    }

}

package org.zalando.riptide;

import org.springframework.http.client.ClientHttpResponse;

public final class NoRoute implements Route {

    private static final Route NO_ROUTE = new NoRoute();

    private NoRoute() {

    }

    @Override
    public void execute(final ClientHttpResponse response, final MessageReader reader) throws Exception {
        throw new UnexpectedResponseException(response);
    }

    public static Route noRoute() {
        return NO_ROUTE;
    }

}

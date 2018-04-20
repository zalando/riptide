package org.zalando.riptide;

import org.apiguardian.api.API;
import org.springframework.http.client.ClientHttpResponse;

import static org.apiguardian.api.API.Status.STABLE;

@API(status = STABLE)
public final class NoRoute implements Route {

    private static final Route NO_ROUTE = new NoRoute();

    private NoRoute() {

    }

    @Override
    public void execute(final ClientHttpResponse response, final MessageReader reader) throws Exception {
        throw new NoRouteException(response);
    }

    public static Route noRoute() {
        return NO_ROUTE;
    }

}

package org.zalando.riptide;

import org.apiguardian.api.API;
import org.springframework.http.client.ClientHttpResponse;

import static org.apiguardian.api.API.Status.STABLE;

@API(status = STABLE)
public final class PassRoute implements Route {

    private static final Route PASS = new PassRoute();

    private PassRoute() {

    }

    @Override
    public void execute(final ClientHttpResponse response, final MessageReader reader) {
        response.close();
    }

    public static Route pass() {
        return PASS;
    }

}

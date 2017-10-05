package org.zalando.riptide;

import org.springframework.http.client.ClientHttpResponse;

public final class PassRoute {

    PassRoute() {
        // package private so we can trick code coverage
    }

    public static Route pass() {
        return Impl.PASS;
    }

    private enum Impl implements Route {
        PASS;

        @Override
        public void execute(final ClientHttpResponse response, final MessageReader reader) throws Exception {
            // nothing to do here
        }
    }

}

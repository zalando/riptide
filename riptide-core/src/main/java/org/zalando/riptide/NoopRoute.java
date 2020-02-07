package org.zalando.riptide;

import org.apiguardian.api.API;
import org.springframework.http.client.ClientHttpResponse;

import static org.apiguardian.api.API.Status.STABLE;

@API(status = STABLE)
public final class NoopRoute implements Route {

    private static final Route NOOP = new NoopRoute();

    private NoopRoute() {

    }

    @Override
    public void execute(
            final ClientHttpResponse response, final MessageReader reader) {
        // nothing to do
    }

    /**
     * Produces a {@link Route} that ignores the response without actively
     * discarding the response body.
     *
     * <strong>Beware</strong> that the returned response needs be closed
     * manually in all cases, otherwise the bound connection may not be
     * released back to the pool.
     *
     * @return a {@link Route route} that doesn't execute any meaningful logic
     */
    public static Route noop() {
        return NOOP;
    }

}

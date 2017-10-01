package org.zalando.riptide;

import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

/**
 * Thrown when no matching {@link Route route} was found during {@link Dispatcher#dispatch(RoutingTree) dispatch}.
 *
 * @see NoRoute#noRoute()
 */
@SuppressWarnings("serial")
public final class NoRouteException extends BaseException {

    private final ClientHttpResponse response;

    public NoRouteException(final ClientHttpResponse response) throws IOException {
        super("Unable to dispatch response", response);
        this.response = response;
    }

    // TODO deprecate?
    public ClientHttpResponse getResponse() {
        return response;
    }

}

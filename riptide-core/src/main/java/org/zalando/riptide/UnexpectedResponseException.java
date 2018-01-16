package org.zalando.riptide;

import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

/**
 * Thrown when no matching {@link Route route} was found during {@link Dispatcher#dispatch(RoutingTree) dispatch}.
 *
 * @see NoRoute#noRoute()
 */
@SuppressWarnings("serial")
public final class UnexpectedResponseException extends HttpResponseException {

    public UnexpectedResponseException(final ClientHttpResponse response) throws IOException {
        super("Unable to dispatch response", response);
    }

}

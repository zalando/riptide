package org.zalando.riptide;

import org.apiguardian.api.*;
import org.springframework.http.client.*;

import java.io.*;

import static org.apiguardian.api.API.Status.*;

/**
 * Thrown when no matching {@link Route route} was found during {@link DispatchStage#dispatch(RoutingTree) dispatch}.
 *
 * @see NoRoute#noRoute()
 */
@API(status = STABLE)
@SuppressWarnings("serial")
public final class UnexpectedResponseException extends HttpResponseException {

    @API(status = INTERNAL)
    public UnexpectedResponseException(final ClientHttpResponse response) throws IOException {
        super("Unable to dispatch response", response);
    }

}

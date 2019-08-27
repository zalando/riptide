package org.zalando.riptide;

import java.net.*;

import static org.zalando.fauxpas.FauxPas.*;

final class NoRouteToHostPlugin implements Plugin {

    @Override
    public RequestExecution aroundDispatch(final RequestExecution execution) {
        return applyTo(execution);
    }

    private RequestExecution applyTo(final RequestExecution execution) {
        return arguments -> execution.execute(arguments)
                .exceptionally(partially(NoRouteToHostException.class, e -> {
                    throw new UnsupportedOperationException(e);
                }));
    }

}

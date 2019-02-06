package org.zalando.riptide;

import java.net.NoRouteToHostException;

import static org.zalando.fauxpas.FauxPas.partially;

final class NoRouteToHostPlugin implements Plugin {

    @Override
    public RequestExecution beforeDispatch(final RequestExecution execution) {
        return applyTo(execution);
    }

    private RequestExecution applyTo(final RequestExecution execution) {
        return arguments -> execution.execute(arguments)
                .exceptionally(partially(NoRouteToHostException.class, e -> {
                    throw new UnsupportedOperationException(e);
                }));
    }

}

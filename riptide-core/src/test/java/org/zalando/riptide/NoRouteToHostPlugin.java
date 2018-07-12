package org.zalando.riptide;

import java.net.NoRouteToHostException;

import static org.zalando.fauxpas.FauxPas.partially;

final class NoRouteToHostPlugin implements Plugin {

    @Override
    public RequestExecution prepare(final RequestArguments arguments,
            final RequestExecution execution) {
        return applyTo(execution);
    }

    private RequestExecution applyTo(final RequestExecution execution) {
        return () -> execution.execute()
                .exceptionally(partially(NoRouteToHostException.class, e -> {
                    throw new UnsupportedOperationException(e);
                }));
    }

}

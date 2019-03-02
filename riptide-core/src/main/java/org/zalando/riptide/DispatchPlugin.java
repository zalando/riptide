package org.zalando.riptide;

import lombok.AllArgsConstructor;

import static org.zalando.fauxpas.FauxPas.throwingFunction;

@AllArgsConstructor
final class DispatchPlugin implements Plugin {

    private final Route route;
    private final MessageReader reader;

    @Override
    public RequestExecution aroundDispatch(final RequestExecution execution) {
        return arguments -> execution.execute(arguments)
                .thenApply(throwingFunction(response -> {
                    try {
                        route.execute(response, reader);
                    } catch (final NoWildcardException e) {
                        throw new UnexpectedResponseException(response);
                    }

                    return response;
                }));
    }

}

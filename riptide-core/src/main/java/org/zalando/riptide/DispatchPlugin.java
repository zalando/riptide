package org.zalando.riptide;

import lombok.*;

import static org.zalando.fauxpas.FauxPas.*;

@AllArgsConstructor
final class DispatchPlugin implements Plugin {

    private final MessageReader reader;

    @Override
    public RequestExecution aroundDispatch(final RequestExecution execution) {
        return arguments -> execution.execute(arguments)
                .thenApply(throwingFunction(response -> {
                    try {
                        arguments.getRoute().execute(response, reader);
                    } catch (final NoWildcardException e) {
                        throw new UnexpectedResponseException(response);
                    }

                    return response;
                }));
    }

}

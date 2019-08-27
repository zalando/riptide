package org.zalando.riptide.chaos;

import lombok.*;
import org.apiguardian.api.*;
import org.zalando.riptide.*;

import static org.apiguardian.api.API.Status.*;

@API(status = EXPERIMENTAL)
@AllArgsConstructor
public final class ChaosPlugin implements Plugin {

    private final FailureInjection injection;

    @Override
    public RequestExecution aroundNetwork(final RequestExecution execution) {
        return injection.inject(execution);
    }

}

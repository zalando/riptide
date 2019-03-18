package org.zalando.riptide.chaos;

import lombok.AllArgsConstructor;
import org.apiguardian.api.API;
import org.zalando.riptide.Plugin;
import org.zalando.riptide.RequestExecution;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;

@API(status = EXPERIMENTAL)
@AllArgsConstructor
public final class ChaosPlugin implements Plugin {

    private final FailureInjection injection;

    @Override
    public RequestExecution aroundNetwork(final RequestExecution execution) {
        return injection.inject(execution);
    }

}

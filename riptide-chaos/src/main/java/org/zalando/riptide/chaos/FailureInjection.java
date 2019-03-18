package org.zalando.riptide.chaos;

import org.apiguardian.api.API;
import org.zalando.riptide.RequestExecution;

import java.util.Arrays;
import java.util.Collection;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;

@API(status = EXPERIMENTAL)
public interface FailureInjection {

    RequestExecution inject(RequestExecution execution);

    static FailureInjection composite(final FailureInjection... injections) {
        return composite(Arrays.asList(injections));
    }

    static FailureInjection composite(final Collection<FailureInjection> injections) {
        return injections.stream()
                .reduce((left, right) ->
                        execution -> right.inject(left.inject(execution)))
                .orElse(execution -> execution);
    }

}

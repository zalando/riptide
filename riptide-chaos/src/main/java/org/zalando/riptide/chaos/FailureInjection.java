package org.zalando.riptide.chaos;

import org.apiguardian.api.*;
import org.zalando.riptide.*;

import java.util.*;

import static org.apiguardian.api.API.Status.*;

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

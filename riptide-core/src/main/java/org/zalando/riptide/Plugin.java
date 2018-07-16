package org.zalando.riptide;

import org.apiguardian.api.API;

import java.util.Arrays;
import java.util.List;

import static org.apiguardian.api.API.Status.STABLE;

/**
 * Plugins allow to modify {@link RequestExecution executions of requests} in order to inject specific behaviour.
 *
 * @see OriginalStackTracePlugin
 */
@API(status = STABLE)
@FunctionalInterface // TODO (3.x): remove
public interface Plugin {

    default RequestExecution interceptBeforeRouting(final RequestArguments arguments, final RequestExecution execution) {
        return execution;
    }

    default RequestExecution interceptAfterRouting(final RequestArguments arguments, final RequestExecution execution) {
        return prepare(arguments, execution);
    }

    // TODO (3.x): remove
    RequestExecution prepare(RequestArguments arguments, RequestExecution execution);

    static Plugin compound(final Plugin... plugins) {
        return compound(Arrays.asList(plugins));
    }

    static Plugin compound(final List<Plugin> plugins) {
        return new CompoundPlugin(plugins);
    }

}

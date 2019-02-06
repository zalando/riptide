package org.zalando.riptide;

import org.apiguardian.api.API;

import java.util.Arrays;
import java.util.List;

import static org.apiguardian.api.API.Status.MAINTAINED;

/**
 * Plugins allow to modify {@link RequestExecution executions of requests} in order to inject specific behaviour.
 *
 * @see OriginalStackTracePlugin
 */
@API(status = MAINTAINED)
public interface Plugin {

    default RequestExecution beforeSend(final RequestExecution execution) {
        return execution;
    }

    default RequestExecution beforeDispatch(final RequestExecution execution) {
        return execution;
    }

    static Plugin compound(final Plugin... plugins) {
        return compound(Arrays.asList(plugins));
    }

    static Plugin compound(final List<Plugin> plugins) {
        return new CompoundPlugin(plugins);
    }

}

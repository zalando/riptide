package org.zalando.riptide;

import java.util.Arrays;
import java.util.List;

/**
 * Plugins allow to modify {@link RequestExecution executions of requests} in order to inject specific behaviour.
 *
 * @see OriginalStackTracePlugin
 */
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
        return plugins.stream().reduce(CompoundPlugin::new).orElse(IdentityPlugin.IDENTITY);
    }

}

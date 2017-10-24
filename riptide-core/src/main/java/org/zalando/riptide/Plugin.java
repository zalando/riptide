package org.zalando.riptide;

import java.util.Arrays;
import java.util.List;

/**
 * Plugins allow to modify {@link RequestExecution executions of requests} in order to inject specific behaviour.
 *
 * @see OriginalStackTracePlugin
 */
@FunctionalInterface // TODO (3.x): remove
public interface Plugin {

    default RequestExecution apply(final RequestArguments arguments, final RequestExecution execution) {
        return execution;
    }

    // TODO (3.x): rename and provide default implementation
    RequestExecution prepare(final RequestArguments arguments, final RequestExecution execution);

    static Plugin compound(final Plugin... plugins) {
        return compound(Arrays.asList(plugins));
    }

    static Plugin compound(final List<Plugin> plugins) {
        return plugins.stream().reduce(CompoundPlugin::new).orElse(IdentityPlugin.IDENTITY);
    }

}

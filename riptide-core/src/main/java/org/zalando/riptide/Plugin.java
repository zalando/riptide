package org.zalando.riptide;

import java.util.Arrays;
import java.util.List;
import java.util.function.BinaryOperator;

/**
 * Plugins allow to modify {@link RequestExecution executions of requests} in order to inject specific behaviour.
 *
 * @see OriginalStackTracePlugin
 */
@FunctionalInterface
public interface Plugin {

    Plugin IDENTITY = (arguments, execution) -> execution;

    RequestExecution prepare(final RequestArguments arguments, final RequestExecution execution);

    static Plugin compound(final Plugin... plugins) {
        return compound(Arrays.asList(plugins));
    }

    static Plugin compound(final List<Plugin> plugins) {
        final BinaryOperator<Plugin> merge = (left, right) ->
                (arguments, execution) ->
                        right.prepare(arguments, left.prepare(arguments, execution));

        return plugins.stream().reduce(merge).orElse(IDENTITY);
    }

}

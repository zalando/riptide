package org.zalando.riptide;

/**
 * Plugins allow to modify {@link RequestExecution executions of requests} in order to inject specific behaviour.
 *
 * @see OriginalStackTracePlugin
 */
@FunctionalInterface
public interface Plugin {

    RequestExecution prepare(final RequestArguments arguments, final RequestExecution execution);

}

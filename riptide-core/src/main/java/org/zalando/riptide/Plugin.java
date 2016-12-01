package org.zalando.riptide;

@FunctionalInterface
public interface Plugin {

    RequestExecution prepare(final RequestArguments arguments, final RequestExecution execution);

    static Plugin merge(final Plugin left, final Plugin right) {
        return (arguments, execution) -> right.prepare(arguments, left.prepare(arguments, execution));
    }

}

package org.zalando.riptide;

enum NoopPlugin implements Plugin {

    INSTANCE;

    @Override
    public RequestExecution prepare(final RequestArguments arguments, final RequestExecution execution) {
        return execution;
    }

}

package org.zalando.riptide;

enum IdentityPlugin implements Plugin {

    IDENTITY;

    @Override
    public RequestExecution interceptBeforeRouting(final RequestArguments arguments, final RequestExecution execution) {
        return execution;
    }

    @Override
    public RequestExecution prepare(final RequestArguments arguments, final RequestExecution execution) {
        return execution;
    }

}

package org.zalando.riptide;

final class CompoundPlugin implements Plugin {

    private final Plugin left;
    private final Plugin right;

    public CompoundPlugin(final Plugin left, final Plugin right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public RequestExecution interceptBeforeRouting(final RequestArguments arguments, final RequestExecution execution) {
        return right.interceptBeforeRouting(arguments, left.interceptBeforeRouting(arguments, execution));
    }

    @Override
    public RequestExecution prepare(final RequestArguments arguments, final RequestExecution execution) {
        return right.prepare(arguments, left.prepare(arguments, execution));
    }

}

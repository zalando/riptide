package org.zalando.riptide;

final class CompoundPlugin implements Plugin {

    private final Plugin left;
    private final Plugin right;

    public CompoundPlugin(final Plugin left, final Plugin right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public RequestExecution beforeSend(final RequestArguments arguments, final RequestExecution execution) {
        return right.beforeSend(arguments, left.beforeSend(arguments, execution));
    }

    @Override
    public RequestExecution beforeDispatch(final RequestArguments arguments, final RequestExecution execution) {
        return right.beforeDispatch(arguments, left.beforeDispatch(arguments, execution));
    }

}

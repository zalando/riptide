package org.zalando.riptide;

final class CompoundPlugin implements Plugin {

    private final Plugin left;
    private final Plugin right;

    public CompoundPlugin(final Plugin left, final Plugin right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public RequestExecution beforeSend(final RequestExecution execution) {
        return right.beforeSend(left.beforeSend(execution));
    }

    @Override
    public RequestExecution beforeDispatch(final RequestExecution execution) {
        return right.beforeDispatch(left.beforeDispatch(execution));
    }

}

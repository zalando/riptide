package org.zalando.riptide;

public final class NoWildcardException extends RuntimeException {

    /**
     * We don't care for the stack trace, and since it's expensive to calculate we just use the same instance
     * all the time. It's exclusively used to jump around the stack.
     */
    @Override
    public Throwable fillInStackTrace() {
        return this;
    }

}

package org.zalando.riptide;

public final class NoWildcardException extends RuntimeException {

    /**
     * We don't care for the stack trace. It's exclusively used to jump around the stack.
     */
    @Override
    public Throwable fillInStackTrace() {
        return this;
    }

}

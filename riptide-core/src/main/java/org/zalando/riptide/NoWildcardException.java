package org.zalando.riptide;

final class NoWildcardException extends RuntimeException {

    /**
     * We don't care for the stack trace, and since it's expensive to calculate we just use the same instance
     * all the time. It's exclusively used to jump around the stack.
     */
    @SuppressWarnings("ThrowableInstanceNeverThrown")
    private static final NoWildcardException INSTANCE = new NoWildcardException();

    private NoWildcardException() {

    }

    static NoWildcardException getInstance() {
        return INSTANCE;
    }

}

package org.zalando.riptide.exceptions;

public final class TemporaryException extends RuntimeException {

    public TemporaryException() {
        // nothing to do
    }

    public TemporaryException(final Throwable cause) {
        super(cause);
    }

}

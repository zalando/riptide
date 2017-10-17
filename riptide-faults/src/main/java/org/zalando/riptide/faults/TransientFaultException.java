package org.zalando.riptide.faults;

public final class TransientFaultException extends RuntimeException {

    public TransientFaultException() {
        // nothing to do
    }

    public TransientFaultException(final Throwable cause) {
        super(cause);
    }

}

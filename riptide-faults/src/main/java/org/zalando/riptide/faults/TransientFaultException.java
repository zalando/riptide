package org.zalando.riptide.faults;

import org.apiguardian.api.API;

import static org.apiguardian.api.API.Status.STABLE;

@API(status = STABLE)
public final class TransientFaultException extends RuntimeException {

    public TransientFaultException() {
        // nothing to do
    }

    public TransientFaultException(final Throwable cause) {
        super(cause);
    }

}

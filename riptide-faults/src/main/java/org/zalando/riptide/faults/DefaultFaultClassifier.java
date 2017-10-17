package org.zalando.riptide.faults;

import com.google.common.base.Throwables;

import java.util.function.Predicate;

final class DefaultFaultClassifier implements FaultClassifier {

    private final Predicate<Throwable> isTransient;

    public DefaultFaultClassifier(final Predicate<Throwable> isTransient) {
        this.isTransient = isTransient;
    }

    @Override
    public Throwable classify(final Throwable throwable) {
        final Throwable root = Throwables.getRootCause(throwable);

        if (isTransient.test(root)) {
            return new TransientFaultException(throwable);
        }

        return throwable;
    }

}

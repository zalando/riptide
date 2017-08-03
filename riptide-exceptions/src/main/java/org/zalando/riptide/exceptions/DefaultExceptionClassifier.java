package org.zalando.riptide.exceptions;

import com.google.common.base.Throwables;

import java.util.function.Predicate;

final class DefaultExceptionClassifier implements ExceptionClassifier {

    private final Predicate<Throwable> isTemporary;

    public DefaultExceptionClassifier(final Predicate<Throwable> isTemporary) {
        this.isTemporary = isTemporary;
    }

    @Override
    public Throwable classify(final Throwable throwable) {
        final Throwable root = Throwables.getRootCause(throwable);

        if (isTemporary.test(root)) {
            return new TemporaryException(throwable);
        }

        return throwable;
    }

}

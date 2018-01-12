package org.zalando.riptide.faults;

import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

final class DefaultFaultClassifier implements FaultClassifier {

    private final Predicate<Throwable> isTransient;

    public DefaultFaultClassifier(final Predicate<Throwable> isTransient) {
        this.isTransient = isTransient;
    }

    @Override
    public Throwable classify(final Throwable throwable) {
        return Stream.iterate(throwable, Throwable::getCause)
                .filter(Objects::nonNull)
                .findFirst()
                .filter(isTransient)
                .<Throwable>map(TransientFaultException::new)
                .orElse(throwable);
    }

}

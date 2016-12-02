package org.zalando.riptide.exceptions;

import javax.net.ssl.SSLHandshakeException;
import java.io.InterruptedIOException;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Predicate;

import static org.zalando.riptide.exceptions.Classification.POTENTIALLY_PERMANENT;
import static org.zalando.riptide.exceptions.Classification.TEMPORARY;

@FunctionalInterface
public interface ExceptionClassifier {

    Classification classify(final Throwable throwable);

    static ExceptionClassifier createDefault() {
        return create(InterruptedIOException.class::isInstance,
                SocketException.class::isInstance,
                SSLHandshakeException.class::isInstance);
    }

    @SafeVarargs
    static ExceptionClassifier create(final Predicate<Throwable>... predicates) {
        return create(Arrays.asList(predicates));
    }

    static ExceptionClassifier create(final Collection<Predicate<Throwable>> predicates) {
        final Predicate<Throwable> predicate = predicates.stream()
                .reduce(Predicate::or)
                .orElse(throwable -> false);

        return throwable -> predicate.test(throwable) ? TEMPORARY : POTENTIALLY_PERMANENT;
    }

}

package org.zalando.riptide.faults;

import lombok.AllArgsConstructor;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.util.function.Predicate;
import java.util.stream.Stream;

@AllArgsConstructor
public final class DefaultFaultClassifier implements FaultClassifier {

    private final ClassificationStrategy strategy;
    private final Predicate<Throwable> included;
    private final Predicate<Throwable> excluded;

    public DefaultFaultClassifier() {
        this(new CausalChainClassificationStrategy());
    }

    public DefaultFaultClassifier(final ClassificationStrategy strategy) {
        this(strategy,
                IOException.class::isInstance,
                merge(
                        UnknownHostException.class::isInstance,
                        throwable -> throwable instanceof SSLException
                                && !(throwable instanceof SSLHandshakeException
                                        && "Remote host closed connection during handshake".equals(throwable.getMessage())),
                        MalformedURLException.class::isInstance
                ));
    }

    @SafeVarargs
    private static Predicate<Throwable> merge(final Predicate<Throwable> predicate,
            final Predicate<Throwable>... predicates) {
        return Stream.of(predicates).reduce(predicate, Predicate::or);
    }

    public DefaultFaultClassifier include(final Predicate<Throwable> predicate) {
        return new DefaultFaultClassifier(strategy, included.or(predicate), excluded);
    }

    public DefaultFaultClassifier exclude(final Predicate<Throwable> predicate) {
        return new DefaultFaultClassifier(strategy, included, excluded.or(predicate));
    }

    @Override
    public Throwable classify(final Throwable throwable) {
        if (throwable instanceof TransientFaultException) {
            return throwable;
        }

        if (!strategy.test(throwable, excluded) && strategy.test(throwable, included)) {
            return new TransientFaultException(throwable);
        }

        return throwable;
    }

}

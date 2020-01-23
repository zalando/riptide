package org.zalando.riptide.faults;

import org.apiguardian.api.API;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import java.io.IOException;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.NoRouteToHostException;
import java.net.UnknownHostException;
import java.util.function.Predicate;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;
import static org.zalando.riptide.faults.ClassificationStrategy.causalChain;
import static org.zalando.riptide.faults.TransientFaults.Rules.transientConnectionFaultRules;
import static org.zalando.riptide.faults.TransientFaults.Rules.transientFaultRules;
import static org.zalando.riptide.faults.TransientFaults.Rules.transientSocketFaultRules;

@API(status = EXPERIMENTAL)
public final class TransientFaults {

    private TransientFaults() {
        // nothing to do
    }

    public static Predicate<Throwable> transientSocketFaults() {
        return transientSocketFaults(causalChain());
    }

    public static Predicate<Throwable> transientSocketFaults(
            final ClassificationStrategy strategy) {

        return combine(strategy, transientSocketFaultRules());
    }

    public static Predicate<Throwable> transientConnectionFaults() {
        return transientConnectionFaults(causalChain());
    }

    public static Predicate<Throwable> transientConnectionFaults(
            final ClassificationStrategy strategy) {

        return combine(strategy, transientConnectionFaultRules());
    }

    public static Predicate<Throwable> combine(
            final ClassificationStrategy strategy,
            final Predicate<Throwable> predicate) {
        return throwable -> strategy.test(throwable, predicate);
    }

    public static Predicate<Throwable> transientFaults() {
        return transientFaults(causalChain());
    }

    public static Predicate<Throwable> transientFaults(
            final ClassificationStrategy strategy) {

        return combine(strategy, transientFaultRules());
    }

    public static final class Rules {

        private Rules() {
            // nothing to do
        }

        public static Rule<Throwable> transientSocketFaultRules() {
            return transientFaultRules()
                    .exclude(transientConnectionFaultRules());
        }

        public static Rule<Throwable> transientConnectionFaultRules() {
            return Rule.of(Predicates.or(
                    ConnectException.class::isInstance,
                    MalformedURLException.class::isInstance,
                    NoRouteToHostException.class::isInstance,
                    UnknownHostException.class::isInstance
            ));
        }

        public static Rule<Throwable> transientFaultRules() {
            return Rule.of(
                    IOException.class::isInstance,
                    throwable -> throwable instanceof SSLException
                            && !(throwable instanceof SSLHandshakeException && "Remote host closed connection during handshake".equals(throwable.getMessage()))
            );
        }

    }

}

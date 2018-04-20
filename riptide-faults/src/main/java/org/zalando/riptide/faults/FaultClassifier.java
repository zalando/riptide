package org.zalando.riptide.faults;

import org.apiguardian.api.API;

import javax.net.ssl.SSLHandshakeException;
import java.io.InterruptedIOException;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Predicate;

import static org.apiguardian.api.API.Status.MAINTAINED;
import static org.apiguardian.api.API.Status.STABLE;

@API(status = STABLE)
public interface FaultClassifier {

    /**
     * Classifies the given {@link Throwable throwable} into transient and persistent faults.
     *
     * @param throwable the throwable
     * @return the given throwable if it's considered permanent, or a {@link TransientFaultException} with the given
     * throwable as its cause, if it's considered transient
     */
    Throwable classify(final Throwable throwable);

    /**
     * Provides a function that classifies its argument using {@link #classify(Throwable)} and throws it. This function
     * is supposed to be used in conjunction with {@link CompletableFuture#exceptionally(Function)}.
     *
     * @see CompletableFuture#exceptionally(Function)
     * @param throwable the throwable
     * @param <T> generic return type
     * @return never, always throws
     * @throws Throwable either argument or wrapped argument
     */
    default <T> T classifyExceptionally(final Throwable throwable) throws Throwable {
        throw classify(throwable);
    }

    @API(status = MAINTAINED)
    static FaultClassifier createDefault() {
        return create(defaults());
    }

    @API(status = MAINTAINED)
    static List<Predicate<Throwable>> defaults()  {
        return Collections.unmodifiableList(Arrays.asList(
                InterruptedIOException.class::isInstance,
                SocketException.class::isInstance,
                throwable -> throwable instanceof SSLHandshakeException
                        && "Remote host closed connection during handshake".equals(throwable.getMessage())));
    }

    @SafeVarargs
    static FaultClassifier create(final Predicate<Throwable>... predicates) {
        return create(Arrays.asList(predicates));
    }

    static FaultClassifier create(final Collection<Predicate<Throwable>> predicates) {
        final Predicate<Throwable> isTransient = predicates.stream().reduce(Predicate::or).orElse(throwable -> false);
        return new DefaultFaultClassifier(isTransient);
    }

}

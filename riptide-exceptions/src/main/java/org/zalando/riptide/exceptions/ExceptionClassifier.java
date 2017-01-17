package org.zalando.riptide.exceptions;

import javax.net.ssl.SSLHandshakeException;
import java.io.InterruptedIOException;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Predicate;

public interface ExceptionClassifier {

    /**
     * Classifies the given {@link Throwable throwable} into temporary and permanent exceptions.
     *
     * @param throwable the throwable
     * @return the given throwable if it's considered permanent, ora {@link TemporaryException} with the given
     * throwable as its cause, if it's considered temporary
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
     */
    <T> T classifyExceptionally(final Throwable throwable);

    static ExceptionClassifier createDefault() {
        return create(InterruptedIOException.class::isInstance,
                SocketException.class::isInstance,
                throwable -> throwable instanceof SSLHandshakeException
                        && "Remote host closed connection during handshake".equals(throwable.getMessage()));
    }

    @SafeVarargs
    static ExceptionClassifier create(final Predicate<Throwable>... predicates) {
        return create(Arrays.asList(predicates));
    }

    static ExceptionClassifier create(final Collection<Predicate<Throwable>> predicates) {
        final Predicate<Throwable> isTemporary = predicates.stream().reduce(Predicate::or).orElse(throwable -> false);

        return new DefaultExceptionClassifier(isTemporary);
    }

}

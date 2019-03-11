package org.zalando.riptide.faults;

import org.apiguardian.api.API;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

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

}

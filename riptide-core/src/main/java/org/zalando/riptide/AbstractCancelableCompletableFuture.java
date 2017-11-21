package org.zalando.riptide;

import java.util.concurrent.CompletableFuture;

import static org.zalando.riptide.CancelableCompletableFuture.preserveCancelability;

public abstract class AbstractCancelableCompletableFuture<T> extends CompletableFuture<T> {

    /**
     * This method is introduced by Java 9 and it's a virtual constructor.
     * This method is especially useful when subclassing CompletableFuture, mainly because it is used internally in
     * almost all methods returning a new CompletionStage, allowing subclasses to control what subtype gets returned
     * by such methods.
     *
     * http://www.baeldung.com/java-9-completablefuture
     */
    // TODO (Java 9): @Override
    public <U> CompletableFuture<U> newIncompleteFuture() {
        return preserveCancelability(this);
    }

}

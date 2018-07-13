package org.zalando.riptide;

import org.apiguardian.api.API;

import java.util.concurrent.CompletableFuture;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;
import static org.zalando.riptide.CancelableCompletableFuture.preserveCancelability;

@API(status = EXPERIMENTAL)
public abstract class AbstractCancelableCompletableFuture<T> extends CompletableFuture<T> {

    /**
     * This method is introduced by Java 9 and it's a virtual constructor.
     * This method is especially useful when subclassing CompletableFuture, mainly because it is used internally in
     * almost all methods returning a new CompletionStage, allowing subclasses to control what subtype gets returned
     * by such methods.
     *
     * http://www.baeldung.com/java-9-completablefuture
     *
     * @param <U> generic future return type
     * @return a new incomplete future to be used when constructing a dependent future
     */
    // TODO (Java 9): @Override
    public <U> CompletableFuture<U> newIncompleteFuture() {
        return preserveCancelability(this);
    }

}

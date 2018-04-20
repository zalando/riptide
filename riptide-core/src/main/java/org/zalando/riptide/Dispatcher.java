package org.zalando.riptide;

import org.apiguardian.api.API;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static java.util.Arrays.asList;
import static org.apiguardian.api.API.Status.STABLE;

@API(status = STABLE)
public abstract class Dispatcher {

    @SafeVarargs
    public final <A> CompletableFuture<Void> dispatch(final Navigator<A> selector, final Binding<A>... bindings) {
        return dispatch(selector, asList(bindings));
    }

    public final <A> CompletableFuture<Void> dispatch(final Navigator<A> selector, final List<Binding<A>> bindings) {
        return dispatch(RoutingTree.dispatch(selector, bindings));
    }

    public <A> CompletableFuture<Void> dispatch(final RoutingTree<A> tree) {
        return call(tree);
    }

    public abstract CompletableFuture<Void> call(final Route route);

}

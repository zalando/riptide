package org.zalando.riptide;

import org.apiguardian.api.*;
import org.springframework.http.client.*;

import java.util.*;
import java.util.concurrent.*;

import static java.util.Arrays.*;
import static org.apiguardian.api.API.Status.*;

@API(status = STABLE)
public abstract class DispatchStage {

    @SafeVarargs
    public final <A> CompletableFuture<ClientHttpResponse> dispatch(final Navigator<A> selector, final Binding<A>... bindings) {
        return dispatch(selector, asList(bindings));
    }

    public final <A> CompletableFuture<ClientHttpResponse> dispatch(final Navigator<A> selector, final List<Binding<A>> bindings) {
        return dispatch(RoutingTree.dispatch(selector, bindings));
    }

    public final <A> CompletableFuture<ClientHttpResponse> dispatch(final RoutingTree<A> tree) {
        return call(tree);
    }

    public abstract CompletableFuture<ClientHttpResponse> call(Route route);

}

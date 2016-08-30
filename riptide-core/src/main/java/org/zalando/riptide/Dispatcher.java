package org.zalando.riptide;

import java.util.List;

import static java.util.Arrays.asList;

public abstract class Dispatcher {

    @SafeVarargs
    public final <A> Completion<Void> dispatch(final Navigator<A> selector, final Binding<A>... bindings) {
        return dispatch(selector, asList(bindings));
    }

    public final <A> Completion<Void> dispatch(final Navigator<A> selector, final List<Binding<A>> bindings) {
        return dispatch(RoutingTree.dispatch(selector, bindings));
    }

    public abstract <A> Completion<Void> dispatch(final RoutingTree<A> tree);

}

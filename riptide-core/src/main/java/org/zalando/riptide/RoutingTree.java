package org.zalando.riptide;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static java.util.Arrays.asList;

public interface RoutingTree<A> extends Route {

    Set<A> keySet();

    Optional<Route> get(final A attribute);

    Optional<Route> getWildcard();

    default RoutingTree<A> merge(final Binding<A> binding) {
        return merge(Collections.singletonList(binding));
    }

    RoutingTree<A> merge(final List<Binding<A>> bindings);

    @SafeVarargs
    static <A> RoutingTree<A> dispatch(final Navigator<A> navigator, final Binding<A>... bindings) {
        return dispatch(navigator, asList(bindings));
    }

    static <A> RoutingTree<A> dispatch(final Navigator<A> navigator, final List<Binding<A>> bindings) {
        return new DefaultRoutingTree<>(navigator, bindings);
    }

}

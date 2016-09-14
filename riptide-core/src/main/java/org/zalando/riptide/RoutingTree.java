package org.zalando.riptide;

import org.springframework.http.client.ClientHttpResponse;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static java.util.Arrays.asList;

public interface RoutingTree<A> extends Route {

    Navigator<A> getNavigator();

    Set<A> keySet();

    Optional<Route> get(final A attribute);

    Optional<Route> getWildcard();

    /**
     * @throws NoWildcardException if no route, not even a wildcard, exists for the given response
     */
    @Override
    void execute(final ClientHttpResponse response, final MessageReader reader) throws Exception;

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

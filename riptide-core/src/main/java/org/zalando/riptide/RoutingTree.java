package org.zalando.riptide;

import org.apiguardian.api.API;
import org.springframework.http.client.ClientHttpResponse;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static java.util.Arrays.asList;
import static org.apiguardian.api.API.Status.EXPERIMENTAL;
import static org.apiguardian.api.API.Status.STABLE;

/**
 * A routing tree is a nested {@link Route route} that consists of a {@link Navigator navigator} and a set of
 * {@link Binding bindings}. When being {@link Route#execute(ClientHttpResponse, MessageReader) executed} the navigator
 * will select the attribute value of the returned {@link ClientHttpResponse response}, find the correct binding
 * and execute {@link Binding#getRoute() it's route}. Since a routing tree is a route itself they can be nested
 * recursively inside each other to produce complex graphs.
 *
 * @param <A> generic attribute type
 * @see Route
 */
@API(status = STABLE)
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

    @API(status = EXPERIMENTAL)
    default RoutingTree<A> merge(final Binding<A> binding) {
        return merge(Collections.singletonList(binding));
    }

    @API(status = EXPERIMENTAL)
    RoutingTree<A> merge(final List<Binding<A>> bindings);

    @SafeVarargs
    static <A> RoutingTree<A> dispatch(final Navigator<A> navigator, final Binding<A>... bindings) {
        return dispatch(navigator, asList(bindings));
    }

    static <A> RoutingTree<A> dispatch(final Navigator<A> navigator, final List<Binding<A>> bindings) {
        return new DefaultRoutingTree<>(navigator, bindings);
    }

}

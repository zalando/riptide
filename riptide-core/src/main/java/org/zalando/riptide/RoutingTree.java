package org.zalando.riptide;

import org.apiguardian.api.*;
import org.springframework.http.client.*;

import java.util.*;

import static java.util.Arrays.*;
import static java.util.stream.Collectors.*;
import static org.apiguardian.api.API.Status.*;

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
    default RoutingTree<A> merge(final List<Binding<A>> bindings) {
        return merge(dispatch(getNavigator(), bindings));
    }

    @Override
    default Route merge(final Route route) {
        if (route instanceof RoutingTree) {
            final RoutingTree other = (RoutingTree) route;
            if (getNavigator().equals(other.getNavigator())) {
                @SuppressWarnings("unchecked")
                final RoutingTree<A> tree = other;
                return merge(tree);
            }
        }

        return Route.super.merge(route);
    }

    @API(status = EXPERIMENTAL)
    default RoutingTree<A> merge(final RoutingTree<A> other) {
        final Map<A, Route> bindings = new LinkedHashMap<>(keySet().size() + other.keySet().size());

        keySet().forEach(attribute ->
                bindings.merge(attribute, get(attribute)
                        .orElseThrow(IllegalStateException::new), Route::merge));

        getWildcard().ifPresent(route ->
                bindings.merge(null, route, Route::merge));

        other.keySet().forEach(attribute ->
                bindings.merge(attribute, other.get(attribute)
                        .orElseThrow(IllegalStateException::new), Route::merge));

        other.getWildcard().ifPresent(route ->
                bindings.merge(null, route, Route::merge));

        return dispatch(getNavigator(), bindings.entrySet().stream()
                .map(e -> Binding.create(e.getKey(), e.getValue()))
                .collect(toList()));
    }

    @SafeVarargs
    static <A> RoutingTree<A> dispatch(final Navigator<A> navigator, final Binding<A>... bindings) {
        return dispatch(navigator, asList(bindings));
    }

    static <A> RoutingTree<A> dispatch(final Navigator<A> navigator, final List<Binding<A>> bindings) {
        return new DefaultRoutingTree<>(navigator, bindings);
    }

}

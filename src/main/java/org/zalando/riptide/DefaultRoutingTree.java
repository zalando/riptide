package org.zalando.riptide;

/*
 * ⁣​
 * Riptide
 * ⁣⁣
 * Copyright (C) 2015 - 2016 Zalando SE
 * ⁣⁣
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ​⁣
 */

import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static java.util.Collections.unmodifiableMap;
import static java.util.stream.Collectors.toMap;

final class DefaultRoutingTree<A> implements RoutingTree<A> {

    private final Navigator<A> navigator;
    private final Map<A, Route> routes;
    private final Optional<Route> wildcard;

    DefaultRoutingTree(final Navigator<A> navigator, final List<Binding<A>> bindings) {
        this(navigator, map(bindings));
    }

    private DefaultRoutingTree(final Navigator<A> navigator, final Map<A, Route> routes) {
        this(navigator, unmodifiableMap(routes), Optional.ofNullable(routes.remove(null)));
    }

    private DefaultRoutingTree(final Navigator<A> navigator, final Map<A, Route> routes, final Optional<Route> wildcard) {
        this.navigator = navigator;
        this.routes = routes;
        this.wildcard = wildcard;
    }

    @Override
    public Set<A> keySet() {
        return routes.keySet();
    }

    @Override
    public Optional<Route> get(final A attribute) {
        return Optional.ofNullable(routes.get(attribute));
    }

    @Override
    public Optional<Route> getWildcard() {
        return wildcard;
    }

    @Override
    public RoutingTree<A> merge(final List<Binding<A>> bindings) {
        final List<Binding<A>> present = new ArrayList<>(routes.size() + 1);
        routes.forEach((attribute, route) -> present.add(Binding.create(attribute, route)));
        wildcard.ifPresent(route -> present.add(Binding.create(null, route)));
        return RoutingTree.create(navigator, navigator.merge(present, bindings));
    }

    private static <A> Map<A, Route> map(final List<Binding<A>> bindings) {
        return bindings.stream()
                .collect(toMap(Binding::getAttribute, Binding::getRoute, (u, v) -> {
                            throw new IllegalArgumentException(String.format("Duplicate key %s", u));
                        }, LinkedHashMap::new));
    }

    @Override
    public Capture execute(final ClientHttpResponse response, final MessageReader reader) throws IOException {
        final Optional<Route> route = navigator.navigate(response, this);

        if (route.isPresent()) {
            try {
                return route.get().execute(response, reader);
            } catch (final NoRouteException e) {
                return executeWildcardOrThrow(response, reader, always(e));
            }
        } else {
            return executeWildcardOrThrow(response, reader, () -> new NoRouteException(response));
        }
    }

    private Capture executeWildcardOrThrow(final ClientHttpResponse response,
            final MessageReader reader, final ThrowingSupplier<NoRouteException> e) throws IOException {

        if (wildcard.isPresent()) {
            return wildcard.get().execute(response, reader);
        }

        throw e.get();
    }

    @FunctionalInterface
    private interface ThrowingSupplier<T> {
        T get() throws IOException;
    }

    private static <T> ThrowingSupplier<T> always(final T t) {
        return () -> t;
    }

}

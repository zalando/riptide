package org.zalando.riptide;

/*
 * ⁣​
 * Riptide
 * ⁣⁣
 * Copyright (C) 2015 Zalando SE
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
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.client.RestClientException;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toMap;

public final class Router<A> {

    private final Selector<A> selector;
    private final Map<A, Binding<A>> bindings;

    private Router(Selector<A> selector, Map<A, Binding<A>> bindings) {
        this.selector = selector;
        this.bindings = bindings;
    }

    @SafeVarargs
    public static <A> Router<A> create(final Selector<A> selector, final Binding<A>... bindings) {
        return create(selector, asList(bindings));
    }

    public static <A> Router<A> create(final Selector<A> selector, final Collection<Binding<A>> bindings) {
        return new Router<A>(selector, Router.createMap(bindings));
    }

    private static <A> Map<A, Binding<A>> createMap(Collection<Binding<A>> bindings) {
        return bindings.stream()
                .collect(toMap(Binding::getAttribute, Function.identity(), checkDuplicates()));
    }

    private static <A> BinaryOperator<Binding<A>> checkDuplicates() {
        return (u, v) -> {
            throw new IllegalArgumentException(
                    String.format("Multiple entries with same key: %s",
                            (u.getAttribute() != null) ? u.getAttribute() : "wildcard"));
        };
    }

    final Capture route(final ClientHttpResponse response, final List<HttpMessageConverter<?>> converters) {
        final Binding<A> wildcard = bindings.get(null);

        try {
            final Map<A, Binding<A>> nonWildcards = bindings.entrySet()
                    .stream().filter(not(isWildcard()))
                    .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
            final Binding<A> match = selector.select(response, nonWildcards);

            if (match == null) {
                return routeToWildcardIfPossible(wildcard, response, converters);
            } else {
                try {
                    return match.execute(response, converters);
                } catch (final NoRouteException e) {
                    return bubbleUpToWildcardIfPossible(wildcard, response, converters, e);
                }
            }
        } catch (final IOException e) {
            throw new RestClientException("Unable to execute binding", e);
        }
    }

    private Predicate<Map.Entry<A, Binding<A>>> isWildcard() {
        return entry -> entry.getKey() == null;
    }

    private <T> Predicate<T> not(final Predicate<T> predicate) {
        return predicate.negate();
    }

    private <X> Capture bubbleUpToWildcardIfPossible(final @Nullable Binding<X> wildcard,
            final ClientHttpResponse response, final List<HttpMessageConverter<?>> converters,
            final NoRouteException e) throws IOException {

        try {
            return routeToWildcardIfPossible(wildcard, response, converters);
        } catch (final NoRouteException ignored) {
            // propagating didn't work, preserve original exception
            throw e;
        }
    }

    private <X> Capture routeToWildcardIfPossible(final @Nullable Binding<X> wildcard,
            final ClientHttpResponse response, final List<HttpMessageConverter<?>> converters) throws IOException {

        if (wildcard == null) {
            throw new NoRouteException(response);
        } else {
            return wildcard.execute(response, converters);
        }
    }

    @SafeVarargs
    public final Router<A> add(Binding<A>... bindings) {
        Map<A, Binding<A>> map = Router.createMap(asList(bindings));
        return new Router<A>(selector,
                Stream.concat(this.bindings.entrySet().stream(), map.entrySet().stream())
                        .collect(toMap(Map.Entry::getKey, Map.Entry::getValue, (u, v) -> v)));
    }
}

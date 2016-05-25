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

import lombok.SneakyThrows;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toMap;

public final class Router<A> {

    private final Selector<A> selector;
    private final Map<A, Executor> routes;

    private Router(Selector<A> selector, Map<A, Executor> bindings) {
        this.selector = selector;
        this.routes = bindings;
    }

    @SafeVarargs
    public static <A> Router<A> create(final Selector<A> selector, final Binding<A>... bindings) {
        return create(selector, asList(bindings));
    }

    public static <A> Router<A> create(final Selector<A> selector, final Collection<Binding<A>> bindings) {
        return new Router<A>(selector, Router.createMap(bindings));
    }

    private static <A> Map<A, Executor> createMap(Collection<Binding<A>> bindings) {
        return bindings.stream().collect(toLinkedMap(Binding::getAttribute, Binding::getExecutor));
    }

    private static <T, K, U>
            Collector<T, ?, Map<K, U>> toLinkedMap(Function<? super T, ? extends K> keyMapper,
                    Function<? super T, ? extends U> valueMapper) {
        return toMap(keyMapper, valueMapper, (u, v) -> v, LinkedHashMap::new);
    }

    final Capture route(final ClientHttpResponse response, final List<HttpMessageConverter<?>> converters) {
        try {
            final Executor match = selector.select(response, routes);

            if (match == null) {
                return routeToWildcardIfPossible(response, converters);
            } else {
                try {
                    return routeToTarget(match, response, converters);
                } catch (final NoRouteException e) {
                    return bubbleUpToWildcardIfPossible(response, converters, e);
                }
            }
        } catch (final IOException e) {
            throw new RestClientException("Unable to execute binding", e);
        }
    }

    @SneakyThrows(Exception.class)
    private Capture routeToTarget(final Executor executor, final ClientHttpResponse response,
            final List<HttpMessageConverter<?>> converters) {
        return  executor.execute(response, converters);
    }

    private <X> Capture routeToWildcardIfPossible(final ClientHttpResponse response, final List<HttpMessageConverter<?>> converters) throws IOException {
        final Executor wildcard = routes.get(null);

        if (wildcard == null) {
            throw new NoRouteException(response);
        } else {
            return routeToTarget(wildcard, response, converters);
        }
    }

    private <X> Capture bubbleUpToWildcardIfPossible(final ClientHttpResponse response, final List<HttpMessageConverter<?>> converters,
            final NoRouteException e) throws IOException {
    
        try {
            return routeToWildcardIfPossible(response, converters);
        } catch (final NoRouteException ignored) {
            // propagating didn't work, preserve original exception
            throw e;
        }
    }

    @SafeVarargs
    public final Router<A> add(Binding<A>... bindings) {
        Map<A, Executor> map = Router.createMap(asList(bindings));
        return new Router<A>(selector,
                Stream.concat(this.routes.entrySet().stream(), map.entrySet().stream())
                        .collect(toLinkedMap(Map.Entry::getKey, Map.Entry::getValue)));
    }
}

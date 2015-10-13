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

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.lang.String.format;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

final class Router {

    private static final Optional WILDCARD = Optional.empty();

    final <A> Capture route(final ClientHttpResponse response, final List<HttpMessageConverter<?>> converters,
            final Selector<A> selector, final Collection<Binding<A>> bindings) {

        final Optional<A> attribute;

        try {
            attribute = selector.attributeOf(response);
        } catch (final IOException e) {
            throw new RestClientException("Unable to retrieve attribute of response", e);
        }

        final Map<Optional<A>, Binding<A>> index = bindings.stream()
                .collect(toMap(Binding::getAttribute, identity(), this::denyDuplicates, LinkedHashMap::new));

        final Optional<Binding<A>> match = selector.select(attribute, index);

        try {
            if (match.isPresent()) {
                final Binding<A> binding = match.get();

                try {
                    return binding.execute(response, converters);
                } catch (final NoRouteException e) {
                    return propagateNoMatch(response, converters, attribute, index, e);
                }
            } else {
                return routeNone(response, converters, attribute, index);
            }
        } catch (final IOException e) {
            throw new RestClientException("Unable to execute binding", e);
        }
    }

    private <A> Binding<A> denyDuplicates(final Binding<A> left, final Binding<A> right) {
        left.getAttribute().ifPresent(a -> {
            throw new IllegalStateException("Duplicate condition attribute: " + a);
        });

        throw new IllegalStateException("Duplicate any conditions");
    }

    private <A> Capture propagateNoMatch(final ClientHttpResponse response,
            final List<HttpMessageConverter<?>> converters, final Optional<A> attribute,
            final Map<Optional<A>, Binding<A>> bindings, final NoRouteException e) throws IOException {
        try {
            return routeNone(response, converters, attribute, bindings);
        } catch (final NoRouteException ignored) {
            // propagating didn't work, preserve original exception
            throw e;
        }
    }

    private <A> Capture routeNone(final ClientHttpResponse response, final List<HttpMessageConverter<?>> converters,
            final Optional<A> attribute, final Map<Optional<A>, Binding<A>> bindings) throws IOException {

        if (containsWildcard(bindings)) {
            final Binding<A> binding = getWildcard(bindings);
            return binding.execute(response, converters);
        } else {
            throw new NoRouteException(formatMessage(attribute, bindings), response);
        }
    }

    private <A> boolean containsWildcard(final Map<Optional<A>, Binding<A>> bindings) {
        return bindings.containsKey(WILDCARD);
    }

    private <A> Binding<A> getWildcard(final Map<Optional<A>, Binding<A>> bindings) {
        return bindings.get(WILDCARD);
    }

    private <A> String formatMessage(final Optional<A> attribute, final Map<Optional<A>, Binding<A>> bindings) {
        return format("Unable to dispatch %s onto %s",
                attribute.map(Object::toString).orElse("none"),
                bindings.keySet().stream().map(this::toName).collect(toList()));
    }

    private String toName(final Optional<?> attribute) {
        return attribute.map(Object::toString).orElse("any");
    }

}

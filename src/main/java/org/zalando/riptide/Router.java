package org.zalando.riptide;

/*
 * ⁣​
 * riptide
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

    private static final Optional ANY = Optional.empty();

    final <A> Captured route(final ClientHttpResponse response, final List<HttpMessageConverter<?>> converters,
            final Selector<A> selector, final Collection<Binding<A>> bindings) throws IOException {

        final Optional<A> attribute = selector.attributeOf(response);
        final Map<Optional<A>, Binding<A>> index = bindings.stream()
                .collect(toMap(Binding::getAttribute, identity(), this::denyDuplicates, LinkedHashMap::new));

        final Optional<Binding<A>> match = selector.select(attribute, index);

        if (match.isPresent()) {
            try {
                final Binding<A> binding = match.get();
                return binding.execute(response, converters);
            } catch (final RestClientDispatchException e) {
                return propagateNoMatch(response, converters, attribute, index, e);
            }
        } else {
            return routeNone(response, converters, attribute, index);
        }
    }

    private <A> Binding<A> denyDuplicates(final Binding<A> left, final Binding<A> right) {
        left.getAttribute().ifPresent(a -> {
            throw new IllegalStateException("Duplicate condition attribute: " + a);
        });

        throw new IllegalStateException("Duplicate any conditions");
    }

    private <A> Captured propagateNoMatch(final ClientHttpResponse response,
            final List<HttpMessageConverter<?>> converters, final Optional<A> attribute,
            final Map<Optional<A>, Binding<A>> index, final RestClientDispatchException e) {
        try {
            return routeNone(response, converters, attribute, index);
        } catch (final RestClientDispatchException ignored) {
            // propagating didn't work, preserve original exception
            throw e;
        }
    }

    private <A> Captured routeNone(final ClientHttpResponse response, final List<HttpMessageConverter<?>> converters,
            final Optional<A> attribute, final Map<Optional<A>, Binding<A>> bindings) {

        if (containsWildcard(bindings)) {
            final Binding<A> binding = getWildcard(bindings);
            return binding.execute(response, converters);
        } else {
            throw new RestClientDispatchException(formatMessage(attribute, bindings), response);
        }
    }

    private <A> boolean containsWildcard(final Map<Optional<A>, Binding<A>> bindings) {
        return bindings.containsKey(ANY);
    }

    private <A> Binding<A> getWildcard(final Map<Optional<A>, Binding<A>> bindings) {
        return bindings.get(ANY);
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

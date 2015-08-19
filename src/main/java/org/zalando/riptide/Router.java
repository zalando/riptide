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
import java.util.function.Function;

import static java.lang.String.format;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

final class Router {

    private static final Optional ANY = Optional.empty();

    final <A> Captured route(ClientHttpResponse response, List<HttpMessageConverter<?>> converters,
            Selector<A> selector, Collection<Binding<A>> bindings) throws IOException {

        final Optional<A> attribute = selector.attributeOf(response);
        final Map<Optional<A>, Binding<A>> index = bindings.stream()
                .collect(toMap(Binding::getAttribute, identity(), this::denyDuplicates, LinkedHashMap::new));

        final Optional<Binding<A>> match = selector.select(attribute, index);

        if (match.isPresent()) {
            try {
                final Binding<A> binding = match.get();
                return binding.execute(response, converters);
            } catch (UnsupportedResponseException e) {
                return propagateNoMatch(response, converters, attribute, index, e);
            } catch (BodyConversionException e) {
                return routeNone(response, converters, attribute, index);
            }
        } else {
            return routeNone(response, converters, attribute, index);
        }
    }

    private <A> Binding<A> denyDuplicates(Binding<A> left, Binding<A> right) {
        left.getAttribute().ifPresent(a -> {
            throw new IllegalStateException("Duplicate condition attribute: " + a);
        });

        throw new IllegalStateException("Duplicate any conditions");
    }

    private <A> Captured propagateNoMatch(ClientHttpResponse response, List<HttpMessageConverter<?>> converters,
            Optional<A> attribute, Map<Optional<A>, Binding<A>> index, UnsupportedResponseException e) throws IOException {
        try {
            return routeNone(response, converters, attribute, index);
        } catch (UnsupportedResponseException ignored) {
            // propagating didn't work, preserve original exception
            throw e;
        }
    }

    private <A> Captured routeNone(ClientHttpResponse response, List<HttpMessageConverter<?>> converters,
            Optional<A> attribute, Map<Optional<A>, Binding<A>> index) throws IOException {

        if (index.containsKey(ANY)) {
            // TODO test exception handling
            return index.get(ANY).execute(response, converters);
        } else {
            final Function<Optional<A>, String> toName = a -> a.map(Object::toString).orElse("any");
            final List<String> attributes = index.keySet().stream().map(toName).collect(toList());
            final String message = format("Unable to dispatch %s onto %s",
                    // TODO there should be a better name than "none"
                    attribute.map(Object::toString).orElse("none"), attributes);

            throw new UnsupportedResponseException(message, response);
        }
    }

}

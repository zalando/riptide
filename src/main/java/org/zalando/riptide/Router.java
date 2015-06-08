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

    final <A> Object route(ClientHttpResponse response, List<HttpMessageConverter<?>> converters,
                           Selector<A> selector, Collection<Binding<A>> bindings) throws IOException {

        final Optional<A> attribute = selector.attributeOf(response);
        final Map<Optional<A>, Binding<A>> index = bindings.stream()
                .collect(toMap(Binding::getAttribute, identity(), (l, r) -> {
                    l.getAttribute().ifPresent(a -> {
                        throw new IllegalStateException("Duplicate condition attribute: " + a);
                    });

                    throw new IllegalStateException("Duplicate any conditions");
                }, LinkedHashMap::new));

        final Optional<Binding<A>> match = selector.select(attribute, index);
        final Optional<A> none = Optional.empty();

        if (match.isPresent()) {
            final Binding<A> binding = match.get();
            return binding.execute(response, converters);
        } else if (index.containsKey(none)) {
            return index.get(none).execute(response, converters);
        } else {
            final List<A> attributes = bindings.stream()
                    .map(Binding::getAttribute)
                    .map(a -> a.orElse(null))
                    .collect(toList());
            final String message = format("Unable to dispatch %s onto %s", attribute.orElse(null), attributes);
            
            throw new UnsupportedResponseException(message, response);
        }
    }

}

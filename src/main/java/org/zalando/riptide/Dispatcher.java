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

import org.springframework.http.HttpMethod;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

public final class Dispatcher {

    private final RestTemplate template;
    private final HttpMethod method;
    private final URI url;
    private final RequestCallback request;

    public Dispatcher(RestTemplate template, HttpMethod method, URI url, RequestCallback request) {
        this.template = template;
        this.method = method;
        this.url = url;
        this.request = request;
    }

    @SafeVarargs
    public final <A> Retriever dispatch(Selector<A> selector, Binding<A>... bindings) {
        final Object value = template.execute(url, method, request, response -> {
            final A attribute = selector.attributeOf(response);

            final Map<A, Binding<A>> index = Stream.of(bindings)
                    .collect(toMap(Binding::getAttribute, identity()));

            final Optional<Binding<A>> match = selector.select(attribute, index);
            
            if (match.isPresent()) {
                final Binding<A> binding = match.get();
                return binding.execute(response, template.getMessageConverters());
            } else {
                throw new RestClientException(format("Unable to dispatch %s onto %s", attribute,
                                        Stream.of(bindings).map(Binding::getAttribute).collect(toList())));
            }
        });

        return new Retriever(value);
    }

}

package org.zalando.riptide;

/*
 * #%L
 * rest-dispatcher
 * %%
 * Copyright (C) 2015 Zalando SE
 * %%
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
 * #L%
 */

import com.google.common.collect.Lists;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.client.HttpMessageConverterExtractor;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.lang.String.format;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

/**
 * A {@link RestDispatcher} creates a {@link ResponseExtractor} that dispatches to one of several
 * functions based on the response's attribute(s).
 * 
 * <p>
 * The attribute which determined how to dispatch is choosen by a {@link Selector} an can be supplied to the 
 * {@link org.zalando.riptide.RestDispatcher.RestDispatcherBuilder#on(Selector)} method. The most useful
 * one are {@link #contentType()} and {@link #statusCode()}.
 * </p>
 * 
 * <p>
 * The functions are supplied via {@link Binding}s which can be constructed using the
 * {@link Binding#consume(Object, Type, Consumer)} and
 * {@link Binding#map(Object, Type, Function)} methods.
 * </p>
 * 
 * A typical example will look like this: 
 * <pre>
 *    template.execute("http://...", GET, null, from(template).on(statusCode()).dispatchTo(
 *            consume(HttpStatus.OK, Happy.class, this::onSuccess),
 *            consume(HttpStatus.NOT_FOUND, Bad.class, bad -> {
 *                throw new NotFoundException(bad);
 *            })
 *    )); 
 * </pre>
 * 
 * @param <A> generic attribute type parameter
 */
public final class RestDispatcher<A> {

    private final Supplier<List<HttpMessageConverter<?>>> converters;
    private final Selector<A> selector;

    RestDispatcher(Supplier<List<HttpMessageConverter<?>>> converters, Selector<A> selector) {
        this.converters = converters;
        this.selector = selector;
    }

    /**
     * Creates a {@link ResponseExtractor} that dispatches on the attribute selected by the
     * {@link Selector} of {@code this} dispatcher.
     * 
     * @param first first binding
     * @param second second binding
     * @param rest optional additional bindings, might be empty 
     * @param <O> generic output type parameter
     * @return a {@link ResponseExtractor} for the requested output type
     * @throws RestClientException if dispatching failed, due to a missing binding
     * @throws IllegalArgumentException if any attribute value of the given bindings occured more than once
     */
    @SafeVarargs
    public final <O> ResponseExtractor<ResponseEntity<O>> dispatchTo(Binding<A, ?, O> first,
                                                                     Binding<A, ?, O> second,
                                                                     Binding<A, ?, O>... rest) {
        final List<Binding<A, ?, O>> bindings = Lists.asList(first, second, rest);

        ensureUniqueAttributeValues(bindings);

        return response -> {
            final A attribute = selector.attributeOf(response);
            final Map<A, Binding<A, ?, O>> mapping = bindings.stream()
                    .collect(toMap(Binding::getAttribute, identity()));
            final Optional<Binding<A, Object, O>> match = selector.select(attribute, mapping).map(this::cast);

            if (match.isPresent()) {
                final Binding<A, Object, O> binding = match.get();

                final Object input = getOutput(response, binding);

                final O body = binding.apply(input);
                return new ResponseEntity<>(body, response.getHeaders(), response.getStatusCode());
            } else {
                throw new RestClientException(format("Unable to dispatch %s onto %s", attribute,
                        bindings.stream().map(Binding::getAttribute).collect(toList())));
            }
        };
    }

    private <O> void ensureUniqueAttributeValues(Collection<Binding<A, ?, O>> bindings) {
        final List<A> duplicates = bindings.stream()
                .map(Binding::getAttribute)
                .collect(groupingBy(identity(), counting()))
                .entrySet().stream()
                .filter(e -> e.getValue() > 1)
                .map(Map.Entry::getKey)
                .collect(toList());

        if (duplicates.isEmpty()) {
            return;
        }
        
        throw new IllegalArgumentException(
                format("Binding attributes have to be unique, got duplicates: %s", duplicates));
    }

    private <I, O> I getOutput(ClientHttpResponse response, Binding<A, I, O> binding) throws java.io.IOException {
        final Type type = binding.getType();
        final ResponseExtractor<I> extractor = new HttpMessageConverterExtractor<>(type, converters.get());
        return extractor.extractData(response);
    }

    @SuppressWarnings("unchecked")
    private <O> Binding<A, Object, O> cast(Binding<A, ?, O> b) {
        return (Binding<A, Object, O>) b;
    }

    public static RestDispatcherBuilder from(RestTemplate template) {
        return from(template::getMessageConverters);
    }
    
    public static RestDispatcherBuilder from(Supplier<List<HttpMessageConverter<?>>> converters) {
        return new RestDispatcherBuilder(converters);
    }

    /**
     * A {@link Selector} that selects the best binding based on the response's content type.
     * 
     * @return a Content-Type selector
     */
    public static Selector<MediaType> contentType() {
        return new ContentTypeSelector();
    }

    /**
     * A {@link Selector} that selects a binding based on the response's status code.
     * 
     * @return an HTTP status code selector
     */
    public static Selector<HttpStatus> statusCode() {
        return new StatusCodeSelector();
    }

    public static final class RestDispatcherBuilder {
    
        private final Supplier<List<HttpMessageConverter<?>>> converters;
    
        RestDispatcherBuilder(Supplier<List<HttpMessageConverter<?>>> converters) {
            this.converters = converters;
        }
        
        public <A> RestDispatcher<A> on(Selector<A> selector) {
            return new RestDispatcher<>(converters, selector);
        }
        
    }
    
}

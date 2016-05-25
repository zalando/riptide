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

import com.google.common.reflect.TypeToken;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.client.HttpMessageConverterExtractor;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.List;
import java.util.function.Function;

import static org.zalando.riptide.Capture.none;

public final class Condition<A> {

    private final A attribute;

    Condition(@Nullable final A attribute) {
        this.attribute = attribute;
    }

    public Binding<A> capture() {
        return bind((response, converters) ->
                Capture.valueOf(response, ClientHttpResponse.class));
    }

    public Binding<A> capture(final ThrowingFunction<ClientHttpResponse, ?> function) {
        return bind((response, converters) ->
                Capture.valueOf(function.apply(response)));
    }

    public Binding<A> call(final ThrowingRunnable consumer) {
        return bind((response, converters) -> {
            consumer.run();
            return none();
        });
    }

    public Binding<A> call(final ThrowingConsumer<ClientHttpResponse> consumer) {
        return bind((response, converters) -> {
            consumer.accept(response);
            return none();
        });
    }

    // TODO ResponseEntity<T> version of this?
    public <I> Binding<A> capture(final Class<I> type) {
        return capture(TypeToken.of(type));
    }

    public <I> Binding<A> capture(final TypeToken<I> type) {
        return bind((response, converters) ->
                Capture.valueOf(convert(type, response, converters), type));
    }

    public <I, T> Binding<A> capture(final Class<I> type, final EntityFunction<I, T> function) {
        return capture(TypeToken.of(type), function);
    }

    public <I, T> Binding<A> capture(final TypeToken<I> type, final EntityFunction<I, T> function) {
        return bind((response, converters) -> {
            final I entity = convert(type, response, converters);
            return Capture.valueOf(function.apply(entity));
        });
    }

    public <I, T> Binding<A> capture(final Class<I> type, final ResponseEntityFunction<I, T> function) {
        return capture(TypeToken.of(type), function);
    }

    public <I, T> Binding<A> capture(final TypeToken<I> type, final ResponseEntityFunction<I, T> function) {
        return bind((response, converters) -> {
            final I entity = convert(type, response, converters);
            return Capture.valueOf(function.apply(toResponseEntity(entity, response)));
        });
    }

    public <I> Binding<A> call(final Class<I> type, final EntityConsumer<I> consumer) {
        return call(TypeToken.of(type), consumer);
    }

    public <I> Binding<A> call(final TypeToken<I> type, final EntityConsumer<I> consumer) {
        return bind((response, converters) -> {
            final I entity = convert(type, response, converters);
            consumer.accept(entity);
            return none();
        });
    }

    public <I> Binding<A> call(final Class<I> type, final ResponseEntityConsumer<I> consumer) {
        return call(TypeToken.of(type), consumer);
    }

    public <I> Binding<A> call(final TypeToken<I> type, final ResponseEntityConsumer<I> consumer) {
        return bind((response, converters) -> {
            final I entity = convert(type, response, converters);
            consumer.accept(toResponseEntity(entity, response));
            return none();
        });
    }

    @SafeVarargs
    public final <B> Binding<A> dispatch(final Selector<B> selector, final Binding<B>... bindings) {
        return dispatch(Router.create(selector, bindings));
    }

    @SafeVarargs
    public final <B> Binding<A> dispatch(final ThrowingFunction<ClientHttpResponse, ClientHttpResponse> function,
            final Selector<B> selector, final Binding<B>... bindings) {
        return dispatch(function, Router.create(selector, bindings));
    }

    public final <B> Binding<A> dispatch(final Router<B> router) {
        return bind((response, converters) ->
                router.route(response, converters));
    }

    public final <B> Binding<A> dispatch(final ThrowingFunction<ClientHttpResponse, ClientHttpResponse> function,
            final Router<B> router) {
        return bind((response, converters) ->
                router.route(function.apply(response), converters));
    }

    public Binding<A> dispatch(final Function<Condition<A>, Binding<A>> tree) {
        return tree.apply(this);
    }

    private Binding<A> bind(final Executor executor) {
        return Binding.create(attribute, executor);
    }

    private static <I> ResponseEntity<I> toResponseEntity(final I entity, final ClientHttpResponse response)
            throws IOException {
        return new ResponseEntity<>(entity, response.getHeaders(), response.getStatusCode());
    }

    private static <I> I convert(final TypeToken<I> type, final ClientHttpResponse response,
            final List<HttpMessageConverter<?>> converters) throws IOException {
        return new HttpMessageConverterExtractor<I>(type.getType(), converters).extractData(response);
    }
}

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

import com.google.common.reflect.TypeToken;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.client.HttpMessageConverterExtractor;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.util.List;

import static org.zalando.riptide.Captured.wrap;
import static org.zalando.riptide.Captured.wrapNothing;

public final class TypedCondition<A, I> implements Capturer<A> {

    private final A attribute;
    private final TypeToken<I> type;

    public TypedCondition(final A attribute, final TypeToken<I> type) {
        this.attribute = attribute;
        this.type = type;
    }

    private I convert(final ClientHttpResponse response, final List<HttpMessageConverter<?>> converters) throws IOException {
        try {
            return new HttpMessageConverterExtractor<I>(type.getType(), converters).extractData(response);
        } catch (final RestClientException | HttpMessageNotReadableException e) {
            throw new BodyConversionException(e);
        }
    }

    private ResponseEntity<I> toResponseEntity(final I entity, final ClientHttpResponse response) throws IOException {
        return new ResponseEntity<>(entity, response.getHeaders(), response.getStatusCode());
    }

    public Binding<A> call(final EntityConsumer<I, ?> consumer) {
        return Binding.create(attribute, (response, converters) -> {
            final I entity = convert(response, converters);
            consumer.accept(entity);
            return wrapNothing();
        });
    }

    public Binding<A> call(final ResponseEntityConsumer<I, ?> consumer) {
        return Binding.create(attribute, (response, converters) -> {
            final I entity = convert(response, converters);
            consumer.accept(toResponseEntity(entity, response));
            return wrapNothing();
        });
    }

    public Capturer<A> map(final EntityFunction<I, ?, ?> function) {
        return () -> Binding.create(attribute, (response, converters) -> {
            final I entity = convert(response, converters);
            return wrap(function.apply(entity));
        });
    }

    public <T> Capturer<A> map(final EntityFunction<I, T, ?> function, final Class<T> mappedType) {
        return map(function, TypeToken.of(mappedType));
    }

    public <T> Capturer<A> map(final EntityFunction<I, T, ?> function, final TypeToken<T> mappedType) {
        return () -> Binding.create(attribute, (response, converters) -> {
            final I entity = convert(response, converters);
            return wrap(function.apply(entity), mappedType);
        });
    }

    public Capturer<A> map(final ResponseEntityFunction<I, ?, ?> function) {
        return () -> Binding.create(attribute, (response, converters) -> {
            final I entity = convert(response, converters);
            return wrap(function.apply(toResponseEntity(entity, response)));
        });
    }

    public <T> Capturer<A> map(final ResponseEntityFunction<I, T, ?> function, final Class<T> mappedType) {
        return map(function, TypeToken.of(mappedType));
    }

    public <T> Capturer<A> map(final ResponseEntityFunction<I, T, ?> function, final TypeToken<T> mappedType) {
        return () -> Binding.create(attribute, (response, converters) -> {
            final I entity = convert(response, converters);
            return wrap(function.apply(toResponseEntity(entity, response)), mappedType);
        });
    }

    @Override
    public Binding<A> capture() {
        return Binding.create(attribute, ((response, converters) -> wrap(convert(response, converters), type)));
    }

}

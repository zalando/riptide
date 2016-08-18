package org.zalando.riptide;

/*
 * ⁣​
 * Riptide
 * ⁣⁣
 * Copyright (C) 2015 - 2016 Zalando SE
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

import com.google.common.reflect.TypeParameter;
import com.google.common.reflect.TypeToken;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMessage;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.net.URI;
import java.util.List;

@FunctionalInterface
public interface Route {

    void execute(final ClientHttpResponse response, final MessageReader reader) throws Exception;

    static Route call(final ThrowingRunnable consumer) {
        return (response, reader) ->
                consumer.run();
    }

    static Route call(final ThrowingConsumer<ClientHttpResponse> consumer) {
        return (response, reader) ->
                consumer.accept(response);
    }

    static <I> Route call(final Class<I> type, final ThrowingConsumer<I> consumer) {
        return call(TypeToken.of(type), consumer);
    }

    static <I> Route call(final TypeToken<I> type, final ThrowingConsumer<I> consumer) {
        return (response, reader) -> {
            final I body = reader.read(type, response);
            consumer.accept(body);
        };
    }

    static <T> TypeToken<List<T>> listOf(final Class<T> entityType) {
        return listOf(TypeToken.of(entityType));
    }

    @SuppressWarnings("serial")
    static <T> TypeToken<List<T>> listOf(final TypeToken<T> entityType) {
        final TypeToken<List<T>> listType = new TypeToken<List<T>>() {
        };

        final TypeParameter<T> elementType = new TypeParameter<T>() {
        };

        return listType.where(elementType, entityType);
    }

    static <T> TypeToken<ResponseEntity<T>> responseEntityOf(final Class<T> entityType) {
        return responseEntityOf(TypeToken.of(entityType));
    }

    static <T> TypeToken<ResponseEntity<T>> responseEntityOf(final TypeToken<T> entityType) {
        final TypeToken<ResponseEntity<T>> responseEntityType = new TypeToken<ResponseEntity<T>>() {
        };

        final TypeParameter<T> elementType = new TypeParameter<T>() {
        };

        return responseEntityType.where(elementType, entityType);
    }

    static ThrowingConsumer<ClientHttpResponse> pass() {
        return response -> {

        };
    }

    static ThrowingFunction<ClientHttpResponse, HttpHeaders> headers() {
        return HttpMessage::getHeaders;
    }

    static ThrowingFunction<ClientHttpResponse, URI> location() {
        return response ->
                response.getHeaders().getLocation();
    }

    static ThrowingConsumer<ClientHttpResponse> noRoute() {
        return response -> {
            throw new NoRouteException(response);
        };
    }

    static <X extends Exception> ThrowingConsumer<X> propagate() {
        return entity -> {
            if (entity instanceof IOException) {
                throw (IOException) entity;
            } else {
                throw new IOException(entity);
            }
        };
    }

    static <T> Adapter<ClientHttpResponse, T> to(final ThrowingFunction<ClientHttpResponse, T> function) {
        return consumer ->
                response -> consumer.accept(function.apply(response));
    }

    @FunctionalInterface
    interface Adapter<T, R> {
        ThrowingConsumer<T> andThen(final ThrowingConsumer<R> consumer);
    }

}

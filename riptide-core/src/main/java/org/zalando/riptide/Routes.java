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

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMessage;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.net.URI;

// TODO move to PartialBinding?
public final class Routes {

    Routes() {
        // package private so we can trick code coverage
    }

    public static ThrowingConsumer<ClientHttpResponse> pass() {
        return response -> {

        };
    }

    public static ThrowingFunction<ClientHttpResponse, HttpHeaders> headers() {
        return HttpMessage::getHeaders;
    }

    public static ThrowingFunction<ClientHttpResponse, URI> location() {
        return response ->
                response.getHeaders().getLocation();
    }

    public static <X extends Exception> ThrowingConsumer<X> propagate() {
        return entity -> {
            if (entity instanceof IOException) {
                throw (IOException) entity;
            } else {
                throw new IOException(entity);
            }
        };
    }

    public static <T> Adapter<ClientHttpResponse, T> to(final ThrowingFunction<ClientHttpResponse, T> function) {
        return consumer ->
                response -> consumer.accept(function.apply(response));
    }

    @FunctionalInterface
    public interface Adapter<T, R> {

        ThrowingConsumer<T> andThen(final ThrowingConsumer<R> consumer);

    }

}

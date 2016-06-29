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
import java.util.Optional;

import static org.springframework.http.HttpHeaders.CONTENT_LOCATION;

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

    public static ThrowingFunction<ClientHttpResponse, URI> contentLocation() {
        return response ->
                Optional.ofNullable(response.getHeaders().getFirst(CONTENT_LOCATION))
                .map(URI::create)
                .orElse(null);
    }

    public static <X extends Exception> EntityConsumer<X> propagate() {
        return entity -> {
            if (entity instanceof IOException) {
                throw (IOException) entity;
            } else {
                throw new IOException(entity);
            }
        };
    }

}

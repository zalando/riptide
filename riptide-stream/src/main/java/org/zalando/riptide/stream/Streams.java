package org.zalando.riptide.stream;

/*
 * ⁣​
 * Riptide: Stream
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

import org.springframework.http.converter.HttpMessageConverter;
import org.zalando.riptide.ThrowingConsumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.reflect.TypeParameter;
import com.google.common.reflect.TypeToken;

import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Main entry point for <b>Riptide Streams</b> extension to capture arbitrary infinite object streams. It allows to
 * receive infinite streams using application/x-json-stream and application/json-seq format, as well as simple finite
 * streams from lists and arrays. Must be enable by registering the {@link StreamConverter} with Riptide (using
 * {@link Streams#streamConverter()}) and declare a route for your stream that is calling a the stream consumer as
 * follows:
 * 
 * <pre>
 * try (Rest rest = Rest.builder().baseUrl("https://api.github.com").converter(streamConverter()).build()) {
 *     rest.get("/repos/{org}/{repo}/contributors", "zalando", "riptide")
 *             .accept(MediaType.APPLICATION_JSON)
 *             .dispatch(series(),
 *                     on(SUCCESSFUL).call(streamOf(User.class),
 *                             forEach(user -> println(user.login + " (" + user.contributions + ")"))))
 *             .get();
 * }
 * </pre>
 * 
 * <b>Note:</b> The stream converter is an replacement to the default spring JSON converter that does not support
 * streaming, and thus should be not registered together with it.
 */
public final class Streams {

    public static <T> TypeToken<Stream<T>> streamOf(final Class<T> type) {
        return streamOf(TypeToken.of(type));
    }

    @SuppressWarnings("serial")
    public static <T> TypeToken<Stream<T>> streamOf(final TypeToken<T> type) {
        final TypeToken<Stream<T>> listType = new TypeToken<Stream<T>>() {
        };

        final TypeParameter<T> elementType = new TypeParameter<T>() {
        };

        return listType.where(elementType, type);
    }

    @SuppressWarnings("serial")
    private static class UncheckedConsumerException extends RuntimeException {

        public UncheckedConsumerException(Exception cause) {
            super(cause);
        }

        @Override
        public Exception getCause() {
            return (Exception) super.getCause();
        }
    }

    public static <I> ThrowingConsumer<Stream<I>> forEach(final ThrowingConsumer<I> consumer) {
        return (input) -> {
            try {
                if (input == null) {
                    return;
                }
                input.forEach(wrap(consumer));
            } catch (UncheckedConsumerException ex) {
                throw ex.getCause();
            }
        };
    }

    private static <I> Consumer<? super I> wrap(final ThrowingConsumer<I> consumer) throws Exception {
        return (i) -> {
            try {
                consumer.accept(i);
            } catch (Exception ex) {
                throw new UncheckedConsumerException(ex);
            }
        };
    }

    public static HttpMessageConverter<?> streamConverter() {
        return new StreamConverter();
    }

    public static HttpMessageConverter<?> streamConverter(ObjectMapper mapper) {
        return new StreamConverter(mapper);
    }
}

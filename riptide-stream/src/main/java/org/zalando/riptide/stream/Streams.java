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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.reflect.TypeParameter;
import com.google.common.reflect.TypeToken;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.zalando.fauxpas.ThrowingConsumer;
import org.zalando.riptide.Route;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;

/**
 * Main entry point for <b>Riptide Streams</b> extension to capture arbitrary infinite object streams. It allows to
 * receive infinite streams using application/x-json-stream and application/json-seq format, as well as simple finite
 * streams from lists and arrays. The feature must be enabled by registering the {@link StreamConverter} with Riptide
 * (using {@link Streams#streamConverter()}) and declare a route for your stream that is calling a the stream consumer
 * as follows:
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

    /** Default singleton {@link MediaType media type} for application/x-json-stream. */
    public static final MediaType APPLICATION_X_JSON_STREAM = //
            new MediaType("application", "x-json-stream", StandardCharsets.UTF_8);
    /** Default singleton {@link MediaType media type} for application/json-seq. */
    public static final MediaType APPLICATION_JSON_SEQ = //
            new MediaType("application", "json-seq", StandardCharsets.UTF_8);

    /**
     * Creates specialized stream {@link TypeToken type token} for the given element {@link Class class type}. Used to
     * declare the expected stream response {@link TypeToken type token} in Riptide {@link Route route} as follows:
     * 
     * <pre>
     *     on(...).call(streamOf(Result.class),...)
     * </pre>
     * 
     * @param type element class type.
     * @return stream token type.
     */
    public static <T> TypeToken<Stream<T>> streamOf(final Class<T> type) {
        return streamOf(TypeToken.of(type));
    }

    /**
     * Creates specialized stream {@link TypeToken type token} for the given element {@link TypeToken type token}. Used
     * to declare the expected stream response {@link TypeToken type token} in Riptide {@link Route route} as follows:
     * 
     * <pre>
     *     on(...).call(streamOf(resultTypeToken),...)
     * </pre>
     * 
     * @param type element token type.
     * @return stream token type.
     */
    @SuppressWarnings("serial")
    public static <T> TypeToken<Stream<T>> streamOf(final TypeToken<T> type) {
        final TypeToken<Stream<T>> streamType = new TypeToken<Stream<T>>() {
            // no overriding needed.
        };

        final TypeParameter<T> elementType = new TypeParameter<T>() {
            // no overriding needed.
        };

        return streamType.where(elementType, type);
    }

    /**
     * Creates {@link ThrowingConsumer stream consumer} for given {@link ThrowingConsumer element consumer}. Used to
     * standardly wrap a single entity consumer function in a stream consumer function as follows:
     * 
     * <pre>
     *     on(...).call(streamOf(...), forEach(element -> { println(element); }))
     * </pre>
     * 
     * @param consumer element consumer function.
     * @return stream consumer function.
     */
    public static <I, X extends Throwable> ThrowingConsumer<Stream<I>, X> forEach(final ThrowingConsumer<I, X> consumer) {
        return (input) -> {
            if (input == null) {
                return;
            }

            try {
                input.forEach(consumer);
            } finally {
                input.close();
            }
        };
    }

    /**
     * Create default stream converter.
     * 
     * @return default stream converter.
     */
    public static HttpMessageConverter<?> streamConverter() {
        return new StreamConverter<>(null, null);
    }

    /**
     * Create stream converter with custom {@link ObjectMapper object mapper).
     * 
     * @param mapper custom {@link ObjectMapper object mapper}.
     * 
     * @return stream converter with customer object mapper.
     */
    public static HttpMessageConverter<?> streamConverter(final ObjectMapper mapper) {
        return new StreamConverter<>(mapper, null);
    }

    /**
     * Create stream converter with custom {@link ObjectMapper object mapper), and custom list of {@link MediaType media
     * types}.
     * 
     * @param mapper custom {@link ObjectMapper object mapper}.
     * @param medias custom list of {@link MediaType media types}.
     * 
     * @return stream converter with customer object mapper.
     */
    public static HttpMessageConverter<?> streamConverter(final ObjectMapper mapper, final List<MediaType> medias) {
        return new StreamConverter<>(mapper, medias);
    }
}

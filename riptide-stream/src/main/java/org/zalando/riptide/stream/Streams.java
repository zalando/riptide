package org.zalando.riptide.stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.reflect.TypeParameter;
import com.google.common.reflect.TypeToken;
import org.apiguardian.api.API;
import org.springframework.http.MediaType;
import org.zalando.fauxpas.ThrowingConsumer;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.apiguardian.api.API.Status.STABLE;

/**
 * Main entry point for <b>Riptide Streams</b> extension to capture arbitrary infinite object streams. It allows to
 * receive infinite streams using application/x-json-stream and application/json-seq format, as well as simple finite
 * streams from lists and arrays. The feature must be enabled by registering the {@link StreamConverter} with Riptide
 * (using {@link Streams#streamConverter(ObjectMapper)} and declare a route for your stream that is calling a the stream consumer
 * as follows:
 * 
 * <pre>{@code
 * try (Http http = Http.builder().baseUrl("https://api.github.com").converter(streamConverter()).build()) {
 *     http.get("/repos/{org}/{repo}/contributors", "zalando", "riptide")
 *             .accept(MediaType.APPLICATION_JSON)
 *             .dispatch(series(),
 *                     on(SUCCESSFUL).call(streamOf(User.class),
 *                             forEach(user -&gt; println(user.login + " (" + user.contributions + ")"))))
 *             .get();
 * }
 * }</pre>
 * 
 * <b>Note:</b> The stream converter is an replacement to the default spring JSON converter that does not support
 * streaming, and thus should be not registered together with it.
 */
@API(status = STABLE)
public final class Streams {

    public static final MediaType APPLICATION_JSON_SEQ = new MediaType("application", "json-seq");
    public static final MediaType APPLICATION_STREAM_JSON = new MediaType("application", "stream+json");
    public static final MediaType APPLICATION_X_JSON_STREAM = new MediaType("application", "x-json-stream");

    private Streams() {

    }

    /**
     * Creates specialized stream {@link TypeToken type token} for the given element {@link Class class type}. Used to
     * declare the expected stream response {@link TypeToken type token} in Riptide route as follows:
     * 
     * <pre>
     *     on(...).call(streamOf(Result.class),...)
     * </pre>
     *
     * @param <T> generic stream element type
     * @param type element class type.
     * @return stream token type.
     */
    public static <T> TypeToken<Stream<T>> streamOf(final Class<T> type) {
        return streamOf(TypeToken.of(type));
    }

    /**
     * Creates specialized stream {@link TypeToken type token} for the given element {@link TypeToken type token}. Used
     * to declare the expected stream response {@link TypeToken type token} in Riptide org.zalando.riptide.Route route} as follows:
     * 
     * <pre>
     *     on(...).call(streamOf(resultTypeToken),...)
     * </pre>
     *
     * @param <T> generic stream element type
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
     * Creates {@link ThrowingConsumer stream consumer} for given {@link ThrowingConsumer element consumer}. Commonly
     * used to wrap a single entity consumer function in a stream consumer function as follows:
     * 
     * <pre>
     *     on(...).call(streamOf(...), forEach(System.out::println))
     * </pre>
     *
     * @param <I> generic stream element type
     * @param <X> generic exception type
     * @param consumer element consumer function.
     * @return stream consumer function.
     */
    public static <I, X extends Throwable> ThrowingConsumer<Stream<I>, X> forEach(final ThrowingConsumer<I, X> consumer) {
        return input -> {
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
     * @param <T> generic stream element type
     * @return default stream converter.
     */
    public static <T> StreamConverter<T> streamConverter() {
        return streamConverter(new ObjectMapper());
    }

    /**
     * Create stream converter with custom {@link ObjectMapper object mapper}.
     *
     * @param <T> generic stream element type
     * @param mapper custom {@link ObjectMapper object mapper}.
     * @return stream converter with customer object mapper.
     */
    public static <T> StreamConverter<T> streamConverter(final ObjectMapper mapper) {
        return streamConverter(mapper, Arrays.asList(
                APPLICATION_JSON_SEQ, APPLICATION_STREAM_JSON, APPLICATION_X_JSON_STREAM));
    }

    /**
     * Create stream converter with custom {@link ObjectMapper object mapper}, and custom list of
     * {@link MediaType supported media types}.
     *
     * @param <T> generic stream element type
     * @param mapper custom {@link ObjectMapper object mapper}.
     * @param supportedMediaTypes custom list of {@link MediaType media types}.
     * @return stream converter with customer object mapper.
     */
    @SuppressWarnings("unchecked")
    public static <T> StreamConverter<T> streamConverter(final ObjectMapper mapper,
            final List<MediaType> supportedMediaTypes) {
        return new StreamConverter(mapper, supportedMediaTypes);
    }
}

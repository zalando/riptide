package org.zalando.riptide.stream;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.GenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.zalando.riptide.stream.Streams.APPLICATION_JSON_SEQ;
import static org.zalando.riptide.stream.Streams.APPLICATION_X_JSON_STREAM;

// TODO change to GenericHttpMessageConverter<Stream<T>>
final class StreamConverter<T> implements GenericHttpMessageConverter<T> {

    private static final List<MediaType> DEFAULT_MEDIA_TYPES =
            Collections.unmodifiableList(Arrays.asList(APPLICATION_JSON_SEQ, APPLICATION_X_JSON_STREAM));

    private final ObjectMapper mapper;
    private final List<MediaType> supportedMediaTypes;

    /**
     * @deprecated Use {@link Streams#streamConverter(ObjectMapper)}
     */
    @Deprecated
    public StreamConverter() {
        this(null, null);
    }

    /**
     * @deprecated Use {@link Streams#streamConverter(ObjectMapper, List)}
     * @param mapper
     * @param supportedMediaTypes
     */
    @Deprecated
    public StreamConverter(@Nullable final ObjectMapper mapper, @Nullable final List<MediaType> supportedMediaTypes) {
        // TODO should not be public and should not have nullable parameters
        this.mapper = mapper != null ? mapper : new ObjectMapper();
        this.supportedMediaTypes = supportedMediaTypes != null ? supportedMediaTypes : DEFAULT_MEDIA_TYPES;
    }

    @Override
    public boolean canRead(final Class<?> clazz, final MediaType mediaType) {
        return this.canRead(clazz, null, mediaType);
    }

    @SuppressWarnings("deprecation")
    private JavaType getJavaType(final Type type, @Nullable final Class<?> contextClass) {
        final TypeFactory tf = mapper.getTypeFactory();
        // Conditional call because Jackson 2.7 does not support null contextClass anymore
        // TypeVariable resolution will not work with Jackson 2.7, see SPR-13853 for more details
        return (contextClass != null) ? tf.constructType(type, contextClass) : tf.constructType(type);
    }

    private boolean canRead(@Nullable final MediaType mediaType) {
        return mediaType == null || getSupportedMediaTypes().stream().anyMatch(mediaType::isCompatibleWith);
    }

    @Override
    public boolean canRead(final Type type, @Nullable final Class<?> contextClass, final MediaType mediaType) {
        final JavaType javaType = this.getJavaType(type, contextClass);
        if (Stream.class.isAssignableFrom(javaType.getRawClass())) {
            final JavaType containedType = javaType.containedType(0);
            return (containedType != null) && mapper.canDeserialize(containedType) && canRead(mediaType);
        }
        return mapper.canDeserialize(javaType) && canRead(mediaType);
    }

    @Override
    public boolean canWrite(final Class<?> clazz, final MediaType mediaType) {
        return false;
    }

    // @Override since 4.2
    public boolean canWrite(final Type type, final Class<?> clazz, final MediaType mediaType) {
        return false;
    }

    @Override
    public List<MediaType> getSupportedMediaTypes() {
        return Collections.unmodifiableList(supportedMediaTypes);
    }

    @Override
    public T read(final Class<? extends T> clazz, final HttpInputMessage inputMessage)
            throws IOException, HttpMessageNotReadableException {
        final JavaType javaType = getJavaType(clazz, null);
        return read(javaType, inputMessage);
    }

    @Override
    public T read(final Type type, final Class<?> contextClass, final HttpInputMessage inputMessage)
            throws IOException, HttpMessageNotReadableException {
        final JavaType javaType = getJavaType(type, contextClass);
        return read(javaType, inputMessage);
    }

    private T read(final JavaType javaType, final HttpInputMessage inputMessage) {
        try {
            if (Stream.class.isAssignableFrom(javaType.getRawClass())) {
                return stream(javaType.containedType(0), input(inputMessage));
            }
            return mapper.readValue(inputMessage.getBody(), javaType);
        } catch (final IOException ex) {
            throw new HttpMessageNotReadableException("Could not read document: " + ex.getMessage(), ex);
        }
    }

    @SuppressWarnings("unchecked")
    private T stream(final JavaType javaType, final InputStream stream) throws IOException {
        final JsonParser parser = mapper.getFactory().createParser(stream);
        final StreamSpliterator<T> split = new StreamSpliterator<>(javaType, parser);
        return (T) StreamSupport.stream(split, false).onClose(() -> {
            try {
                parser.close();
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    private InputStream input(final HttpInputMessage inputMessage) throws IOException {
        final MediaType contentType = inputMessage.getHeaders().getContentType();
        final boolean sequence = APPLICATION_JSON_SEQ.includes(contentType);
        return sequence ? new StreamFilter(inputMessage.getBody()) : inputMessage.getBody();
    }

    @Override
    public void write(final T t, final MediaType mediaType, final HttpOutputMessage message)  {
        throw new UnsupportedOperationException();
    }

    // @Override since 4.2
    public void write(final T t, final Type type, final MediaType mediaType, final HttpOutputMessage message)  {
        throw new UnsupportedOperationException();
    }

}
package org.zalando.riptide.stream;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.GenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.type.TypeFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.zalando.fauxpas.FauxPas.throwingRunnable;
import static org.zalando.riptide.stream.Streams.APPLICATION_JSON_SEQ;

@AllArgsConstructor
final class StreamConverter<T> implements GenericHttpMessageConverter<Stream<T>> {

    private final JsonMapper mapper;
    private final List<MediaType> supportedMediaTypes;

    @Override
    public boolean canRead(final Class<?> clazz, @Nullable final MediaType mediaType) {
        // we only support generics
        return false;
    }

    @Override
    public boolean canRead(final Type type, @Nullable final Class<?> contextClass, @Nullable final MediaType mediaType) {
        final JavaType javaType = getJavaType(type, contextClass);

        if (Stream.class.isAssignableFrom(javaType.getRawClass())) {
            return canRead(mediaType);
        }

        return false;
    }

    private boolean canRead(@Nullable final MediaType mediaType) {
        return mediaType == null || getSupportedMediaTypes().stream().anyMatch(mediaType::isCompatibleWith);
    }

    @Override
    @Nonnull
    public List<MediaType> getSupportedMediaTypes() {
        return Collections.unmodifiableList(supportedMediaTypes);
    }

    @Override
    @Nonnull
    public Stream<T> read(final Class<? extends Stream<T>> clazz, final HttpInputMessage inputMessage)
            throws HttpMessageNotReadableException {
        // we only support generics
        throw new UnsupportedOperationException();
    }

    @Override
    @Nonnull
    public Stream<T> read(final Type type, @Nullable final Class<?> contextClass, final HttpInputMessage inputMessage)
            throws HttpMessageNotReadableException {
        final JavaType javaType = getJavaType(type, contextClass);
        return read(javaType, inputMessage);
    }

    private JavaType getJavaType(final Type type, @Nullable final Class<?> contextClass) {
        final TypeFactory factory = mapper.getTypeFactory();
        return factory.constructType(type);
    }

    private Stream<T> read(final JavaType javaType, final HttpInputMessage inputMessage) {
        try {
            final JavaType elementType = javaType.containedType(0);
            final InputStream body = extractBody(inputMessage);
            return stream(elementType, body);
        } catch (final IOException ex) {
            throw new HttpMessageNotReadableException("Could not read document: " + ex.getMessage(), ex, inputMessage);
        }
    }

    private InputStream extractBody(final HttpInputMessage message) throws IOException {
        final MediaType contentType = message.getHeaders().getContentType();
        final boolean sequence = APPLICATION_JSON_SEQ.includes(contentType);
        return sequence ? new StreamFilter(message.getBody()) : message.getBody();
    }

    private Stream<T> stream(final JavaType elementType, final InputStream stream) throws IOException {
        final JsonParser parser = mapper.createParser(stream);
        final StreamSpliterator<T> split = new StreamSpliterator<>(elementType, parser);
        return StreamSupport.stream(split, false).onClose(throwingRunnable(parser::close));
    }

    @Override
    public boolean canWrite(final Class<?> clazz, @Nullable final MediaType mediaType) {
        return false;
    }

    // @Override since 4.2
    public boolean canWrite(@Nullable final Type type, final Class<?> clazz, @Nullable final MediaType mediaType) {
        return false;
    }

    @Override
    public void write(final Stream<T> t, @Nullable final MediaType mediaType, final HttpOutputMessage message) {
        throw new UnsupportedOperationException();
    }

    // @Override since 4.2
    public void write(final Stream<T> t,
                      @Nullable final Type type,
                      @Nullable final MediaType mediaType,
                      final HttpOutputMessage message) {
        throw new UnsupportedOperationException();
    }

}

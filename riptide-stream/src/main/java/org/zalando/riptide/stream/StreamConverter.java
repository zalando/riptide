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
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.zalando.fauxpas.FauxPas.throwingRunnable;
import static org.zalando.riptide.stream.Streams.APPLICATION_JSON_SEQ;

final class StreamConverter<T> implements GenericHttpMessageConverter<Stream<T>> {

    private final ObjectMapper mapper;
    private final List<MediaType> supportedMediaTypes;

    StreamConverter(final ObjectMapper mapper, final List<MediaType> supportedMediaTypes) {
        this.mapper = mapper;
        this.supportedMediaTypes = supportedMediaTypes;
    }

    @Override
    public boolean canRead(final Class<?> clazz, final MediaType mediaType) {
        // we only support generics
        return false;
    }

    @Override
    public boolean canRead(final Type type, @Nullable final Class<?> contextClass, final MediaType mediaType) {
        final JavaType javaType = getJavaType(type, contextClass);

        if (Stream.class.isAssignableFrom(javaType.getRawClass())) {
            final JavaType containedType = javaType.containedType(0);
            return mapper.canDeserialize(containedType) && canRead(mediaType);
        }

        return false;
    }

    private boolean canRead(@Nullable final MediaType mediaType) {
        return mediaType == null || getSupportedMediaTypes().stream().anyMatch(mediaType::isCompatibleWith);
    }

    @Override
    public List<MediaType> getSupportedMediaTypes() {
        return Collections.unmodifiableList(supportedMediaTypes);
    }

    @Override
    public Stream<T> read(final Class<? extends Stream<T>> clazz, final HttpInputMessage inputMessage)
            throws IOException, HttpMessageNotReadableException {
        // we only support generics
        throw new UnsupportedOperationException();
    }

    @Override
    public Stream<T> read(final Type type, final Class<?> contextClass, final HttpInputMessage inputMessage)
            throws IOException, HttpMessageNotReadableException {
        final JavaType javaType = getJavaType(type, contextClass);
        return read(javaType, inputMessage);
    }

    @SuppressWarnings("deprecation")
    private JavaType getJavaType(final Type type, @Nullable final Class<?> contextClass) {
        final TypeFactory factory = mapper.getTypeFactory();
        // Conditional call because Jackson 2.7 does not support null contextClass anymore
        // TypeVariable resolution will not work with Jackson 2.7, see SPR-13853 for more details
        return contextClass == null ? factory.constructType(type) : factory.constructType(type, contextClass);
    }

    private Stream<T> read(final JavaType javaType, final HttpInputMessage inputMessage) {
        try {
            final JavaType elementType = javaType.containedType(0);
            final InputStream body = extractBody(inputMessage);
            return stream(elementType, body);
        } catch (final IOException ex) {
            throw new HttpMessageNotReadableException("Could not read document: " + ex.getMessage(), ex);
        }
    }

    private InputStream extractBody(final HttpInputMessage message) throws IOException {
        final MediaType contentType = message.getHeaders().getContentType();
        final boolean sequence = APPLICATION_JSON_SEQ.includes(contentType);
        return sequence ? new StreamFilter(message.getBody()) : message.getBody();
    }

    private Stream<T> stream(final JavaType elementType, final InputStream stream) throws IOException {
        final JsonParser parser = mapper.getFactory().createParser(stream);
        final StreamSpliterator<T> split = new StreamSpliterator<>(elementType, parser);
        return StreamSupport.stream(split, false).onClose(throwingRunnable(parser::close));
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
    public void write(final Stream<T> t, final MediaType mediaType, final HttpOutputMessage message)  {
        throw new UnsupportedOperationException();
    }

    // @Override since 4.2
    public void write(final Stream<T> t, final Type type, final MediaType mediaType, final HttpOutputMessage message)  {
        throw new UnsupportedOperationException();
    }

}

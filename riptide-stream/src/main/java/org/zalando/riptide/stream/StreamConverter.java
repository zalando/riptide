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

import static org.zalando.riptide.stream.Streams.APPLICATION_JSON_SEQ;
import static org.zalando.riptide.stream.Streams.APPLICATION_X_JSON_STREAM;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.GenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;

final class StreamConverter<T> implements GenericHttpMessageConverter<T> {

    private static final List<MediaType> DEFAULT_MEDIA_TYPES =
            Arrays.asList(APPLICATION_JSON_SEQ, APPLICATION_X_JSON_STREAM);

    private final ObjectMapper mapper;
    private final List<MediaType> medias;

    public StreamConverter() {
        this(null, null);
    }

    public StreamConverter(final ObjectMapper mapper, final List<MediaType> medias) {
        this.mapper = (mapper != null) ? mapper : Jackson2ObjectMapperBuilder.json().build();
        this.medias = (medias != null) ? medias : DEFAULT_MEDIA_TYPES;
    }

    @Override
    public boolean canRead(Class<?> clazz, MediaType mediaType) {
        return this.canRead(clazz, null, mediaType);
    }

    @SuppressWarnings("deprecation")
    private JavaType getJavaType(final Type type, final Class<?> contextClass) {
        TypeFactory tf = mapper.getTypeFactory();
        // Conditional call because Jackson 2.7 does not support null contextClass anymore
        // TypeVariable resolution will not work with Jackson 2.7, see SPR-13853 for more details
        return (contextClass != null) ? tf.constructType(type, contextClass) : tf.constructType(type);
    }

    private boolean canRead(final MediaType mediaType) {
        return mediaType == null || getSupportedMediaTypes().stream().anyMatch(mediaType::isCompatibleWith);
    }

    @Override
    public boolean canRead(final Type type, final Class<?> contextClass, final MediaType mediaType) {
        final JavaType javaType = this.getJavaType(type, contextClass);
        if (Stream.class.isAssignableFrom(javaType.getRawClass())) {
            JavaType containedType = javaType.containedType(0);
            return (containedType != null) && mapper.canDeserialize(containedType) && canRead(mediaType);
        }
        return mapper.canDeserialize(javaType) && canRead(mediaType);
    }

    @Override
    public boolean canWrite(final Class<?> clazz, final MediaType mediaType) {
        return false;
    }

    @Override
    public boolean canWrite(final Type type, final Class<?> clazz, final MediaType mediaType) {
        return false;
    }

    @Override
    public List<MediaType> getSupportedMediaTypes() {
        return this.medias;
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
        } catch (IOException ex) {
            throw new HttpMessageNotReadableException("Could not read document: " + ex.getMessage(), ex);
        }
    }

    @SuppressWarnings("unchecked")
    private T stream(final JavaType javaType, final InputStream stream)
            throws IOException, JsonParseException {
        final JsonParser parser = mapper.getFactory().createParser(stream);
        final StreamSpliterator<Object> split = new StreamSpliterator<>(javaType, parser, mapper);
        return (T) StreamSupport.stream(split, false).onClose(split::close);
    }

    private InputStream input(final HttpInputMessage inputMessage) throws IOException {
        final MediaType contentType = inputMessage.getHeaders().getContentType();
        final boolean sequence = APPLICATION_JSON_SEQ.includes(contentType);
        return (sequence) ? new StreamFilter(inputMessage.getBody()) : inputMessage.getBody();
    }

    @Override
    public void write(final T t, final MediaType contentType, final HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void write(final T t, final Type type, final MediaType contentType, final HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {
        throw new UnsupportedOperationException();
    }
}
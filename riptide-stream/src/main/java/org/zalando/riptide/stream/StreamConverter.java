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

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

class StreamConverter extends MappingJackson2HttpMessageConverter {

    protected StreamConverter() {
        super();
    }

    protected StreamConverter(ObjectMapper objectMapper) {
        super(objectMapper);
    }

    @Override
    public boolean canRead(Class<?> clazz, MediaType mediaType) {
        return this.canRead(clazz, null, mediaType);
    }

    @Override
    public boolean canRead(Type type, Class<?> contextClass, MediaType mediaType) {
        final JavaType javaType = getJavaType(type, contextClass);
        if (Stream.class.isAssignableFrom(javaType.getRawClass())) {
            return super.canRead(javaType.containedType(0), contextClass, mediaType);
        }
        return super.canRead(javaType, contextClass, mediaType);
    }

    @Override
    public Object read(Type type, Class<?> contextClass, HttpInputMessage inputMessage)
            throws IOException, HttpMessageNotReadableException {

        JavaType javaType = getJavaType(type, contextClass);
        try {
            if (Stream.class.isAssignableFrom(javaType.getRawClass())) {
                final JavaType containedType = javaType.containedType(0);
                final InputStream stream = new StreamFilter(inputMessage.getBody());
                final JsonParser parser = objectMapper.getFactory().createParser(stream);
                return StreamSupport.stream(new StreamSpliterator<>(containedType, parser, objectMapper), false);
            }
            return objectMapper.readValue(inputMessage.getBody(), javaType);
        } catch (IOException ex) {
            throw new HttpMessageNotReadableException("Could not read document: " + ex.getMessage(), ex);
        }
    }
}
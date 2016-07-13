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

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_XML;
import static org.zalando.riptide.stream.Streams.APPLICATION_X_JSON_STREAM;
import static org.zalando.riptide.stream.Streams.APPLICATION_JSON_SEQ;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.zalando.riptide.model.AccountBody;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;

public class StreamConverterTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void shouldSupportRead() throws Exception {
        final ObjectMapper mapper = mock(ObjectMapper.class);
        final TypeFactory factory = new ObjectMapper().getTypeFactory();
        when(mapper.getTypeFactory()).thenReturn(factory);
        when(mapper.canDeserialize(any())).thenReturn(true);
        final StreamConverter<?> unit = new StreamConverter<>(mapper, Arrays.asList(APPLICATION_JSON));

        assertFalse(unit.canRead(Object.class, APPLICATION_XML));
        assertFalse(unit.canRead(Stream.class, APPLICATION_JSON));

        assertTrue(unit.canRead(Object.class, APPLICATION_JSON));
        assertTrue(unit.canRead(List.class, APPLICATION_JSON));
        assertTrue(unit.canRead(List[].class, APPLICATION_JSON));
        assertTrue(unit.canRead(AccountBody.class, null));
        assertTrue(unit.canRead(AccountBody[].class, null));

        when(mapper.canDeserialize(factory.constructType(AccountBody.class))).thenReturn(false);
        assertFalse(unit.canRead(AccountBody.class, APPLICATION_JSON));
    }

    @Test
    public void shouldSupportReadGeneric() throws Exception {
        final ObjectMapper mapper = mock(ObjectMapper.class);
        final TypeFactory factory = new ObjectMapper().getTypeFactory();
        when(mapper.getTypeFactory()).thenReturn(factory);
        when(mapper.canDeserialize(any())).thenReturn(true);
        final StreamConverter<?> unit = new StreamConverter<>(mapper, Arrays.asList(APPLICATION_X_JSON_STREAM));

        assertFalse(unit.canRead(Streams.streamOf(Object.class).getType(), getClass(), APPLICATION_XML));

        assertTrue(unit.canRead(Streams.streamOf(List.class).getType(), getClass(), APPLICATION_X_JSON_STREAM));
        assertTrue(unit.canRead(Streams.streamOf(List[].class).getType(), getClass(), APPLICATION_X_JSON_STREAM));
        assertTrue(unit.canRead(Streams.streamOf(AccountBody.class).getType(), getClass(), null));
        assertTrue(unit.canRead(Streams.streamOf(AccountBody[].class).getType(), getClass(), null));

        when(mapper.canDeserialize(factory.constructType(AccountBody.class))).thenReturn(false);
        assertFalse(unit.canRead(Streams.streamOf(AccountBody.class).getType(), getClass(), APPLICATION_X_JSON_STREAM));
    }

    @Test
    public void shouldNotSupportWrite() throws Exception {
        final StreamConverter<?> unit = new StreamConverter<>();
        assertFalse(unit.canWrite(AccountBody.class, APPLICATION_X_JSON_STREAM));
    }

    @Test
    public void shouldNotSupportWriteGeneric() throws Exception {
        final StreamConverter<?> unit = new StreamConverter<>();
        assertFalse(unit.canWrite(Streams.streamOf(AccountBody.class).getType(), null, APPLICATION_X_JSON_STREAM));
    }

    private HttpInputMessage mockWithContentType(final MediaType mediaType) {
        HttpInputMessage input = mock(HttpInputMessage.class);
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaType);
        when(input.getHeaders()).thenReturn(headers);
        return input;
    }

    @Test
    public void shouldSupportReadItem() throws Exception {
        final StreamConverter<AccountBody> unit =
                new StreamConverter<>(new ObjectMapper().findAndRegisterModules(),
                        Arrays.asList(APPLICATION_JSON));
        final HttpInputMessage input = mockWithContentType(APPLICATION_JSON);
        when(input.getBody()).thenReturn(new ClassPathResource("account-item.json").getInputStream());

        assertThat(unit.read(AccountBody.class, input), is(new AccountBody("1234567890", "Acme Corporation")));
    }

    @Test
    public void shouldSupportReadStream() throws Exception {
        Type type = Streams.streamOf(AccountBody.class).getType();
        final StreamConverter<Stream<AccountBody>> unit =
                new StreamConverter<>(new ObjectMapper().findAndRegisterModules(),
                        Arrays.asList(APPLICATION_X_JSON_STREAM));
        final HttpInputMessage input = mockWithContentType(APPLICATION_X_JSON_STREAM);
        when(input.getBody()).thenReturn(new ClassPathResource("account-stream.json").getInputStream());

        Stream<AccountBody> stream = unit.read(type, null, input);

        @SuppressWarnings("unchecked")
        final Consumer<? super AccountBody> verifier = mock(Consumer.class);

        stream.forEach(verifier);

        verify(verifier).accept(new AccountBody("1234567890", "Acme Corporation"));
        verify(verifier).accept(new AccountBody("1234567891", "Acme Company"));
        verify(verifier).accept(new AccountBody("1234567892", "Acme GmbH"));
        verify(verifier).accept(new AccountBody("1234567893", "Acme SE"));
        verify(verifier, times(4)).accept(any(AccountBody.class));
    }

    @Test
    public void shouldSupportReadSequence() throws Exception {
        Type type = Streams.streamOf(AccountBody.class).getType();
        final StreamConverter<Stream<AccountBody>> unit =
                new StreamConverter<>(new ObjectMapper().findAndRegisterModules(),
                        Arrays.asList(APPLICATION_JSON_SEQ));
        final HttpInputMessage input = mockWithContentType(APPLICATION_JSON_SEQ);
        when(input.getBody()).thenReturn(new ClassPathResource("account-sequence.json").getInputStream());

        Stream<AccountBody> stream = unit.read(type, null, input);

        @SuppressWarnings("unchecked")
        final Consumer<? super AccountBody> verifier = mock(Consumer.class);

        stream.forEach(verifier);

        verify(verifier).accept(new AccountBody("1234567890", "Acme Corporation"));
        verify(verifier).accept(new AccountBody("1234567891", "Acme Company"));
        verify(verifier).accept(new AccountBody("1234567892", "Acme GmbH"));
        verify(verifier).accept(new AccountBody("1234567893", "Acme SE"));
        verify(verifier, times(4)).accept(any(AccountBody.class));
    }

    @Test
    public void shouldFailOnReadForIOException() throws Exception {
        exception.expect(HttpMessageNotReadableException.class);
        exception.expectCause(instanceOf(IOException.class));

        final StreamConverter<?> unit = new StreamConverter<>();
        final HttpInputMessage input = mockWithContentType(APPLICATION_X_JSON_STREAM);

        doThrow(new IOException()).when(input).getBody();

        unit.read(Streams.streamOf(Object.class).getType(), null, input);
    }

    @Test
    public void writeNotSupported() throws Exception {
        exception.expect(UnsupportedOperationException.class);

        final StreamConverter<?> unit = new StreamConverter<>();
        unit.write(null, APPLICATION_X_JSON_STREAM, null);
    }

    @Test
    public void writeGenericNotSupported() throws Exception {
        exception.expect(UnsupportedOperationException.class);

        final StreamConverter<?> unit = new StreamConverter<>();
        unit.write(null, Streams.streamOf(AccountBody.class).getType(), APPLICATION_X_JSON_STREAM, null);
    }
}
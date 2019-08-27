package org.zalando.riptide.stream;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.type.*;
import org.junit.jupiter.api.*;
import org.springframework.core.io.*;
import org.springframework.http.*;
import org.springframework.http.converter.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import static java.util.Collections.*;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.*;
import static org.zalando.riptide.stream.Streams.*;

final class StreamConverterTest {

    @Test
    void shouldSupportMediaTypes() {
        final List<MediaType> medias = streamConverter().getSupportedMediaTypes();
        assertThat(medias, hasItem(APPLICATION_X_JSON_STREAM));
        assertThat(medias, hasItem(APPLICATION_JSON_SEQ));
    }

    @Test
    void shouldSupportRead() {
        final HttpMessageConverter<Stream<Object>> unit = streamConverter(new ObjectMapper(),
                singletonList(APPLICATION_JSON));

        assertFalse(unit.canRead(Object.class, APPLICATION_XML));
        assertFalse(unit.canRead(Stream.class, APPLICATION_JSON));
        assertFalse(unit.canRead(Object.class, APPLICATION_JSON));
        assertFalse(unit.canRead(List.class, APPLICATION_JSON));
        assertFalse(unit.canRead(List[].class, APPLICATION_JSON));
        assertFalse(unit.canRead(AccountBody.class, null));
        assertFalse(unit.canRead(AccountBody[].class, null));
    }

    @Test
    void shouldSupportReadGeneric() {
        final ObjectMapper mapper = mock(ObjectMapper.class);
        final TypeFactory factory = new ObjectMapper().getTypeFactory();
        when(mapper.getTypeFactory()).thenReturn(factory);
        when(mapper.canDeserialize(any())).thenReturn(true);
        final StreamConverter<Object> unit = streamConverter(mapper, singletonList(APPLICATION_X_JSON_STREAM));

        assertFalse(unit.canRead(Object.class, getClass(), APPLICATION_X_JSON_STREAM));
        assertFalse(unit.canRead(Streams.streamOf(Object.class).getType(), getClass(), APPLICATION_XML));

        assertTrue(unit.canRead(Streams.streamOf(List.class).getType(), getClass(), APPLICATION_X_JSON_STREAM));
        assertTrue(unit.canRead(Streams.streamOf(List[].class).getType(), getClass(), APPLICATION_X_JSON_STREAM));
        assertTrue(unit.canRead(Streams.streamOf(AccountBody.class).getType(), getClass(), null));
        assertTrue(unit.canRead(Streams.streamOf(AccountBody[].class).getType(), getClass(), null));

        when(mapper.canDeserialize(factory.constructType(AccountBody.class))).thenReturn(false);
        assertFalse(unit.canRead(Streams.streamOf(AccountBody.class).getType(), getClass(), APPLICATION_X_JSON_STREAM));
    }

    @Test
    void shouldNotSupportWrite() {
        final HttpMessageConverter<Stream<AccountBody>> unit = streamConverter();
        assertFalse(unit.canWrite(AccountBody.class, APPLICATION_X_JSON_STREAM));
    }

    @Test
    void shouldNotSupportWriteGeneric() {
        final StreamConverter<AccountBody> unit = streamConverter();
        assertFalse(unit.canWrite(Streams.streamOf(AccountBody.class).getType(), null, APPLICATION_X_JSON_STREAM));
    }

    private HttpInputMessage mockWithContentType(final MediaType mediaType) {
        final HttpInputMessage input = mock(HttpInputMessage.class);
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaType);
        when(input.getHeaders()).thenReturn(headers);
        return input;
    }

    @SuppressWarnings("unchecked")
    @Test
    void shouldNotSupportReadStream() throws Exception {
        final StreamConverter unit =
                new StreamConverter<>(new ObjectMapper().findAndRegisterModules(),
                        singletonList(APPLICATION_X_JSON_STREAM));
        final HttpInputMessage input = mockWithContentType(APPLICATION_X_JSON_STREAM);
        when(input.getBody()).thenReturn(new ClassPathResource("account-stream.json").getInputStream());

        assertThrows(UnsupportedOperationException.class, () -> unit.read(Stream.class, input));
    }

    @Test
    void shouldSupportGenericReadStream() throws Exception {
        final Type type = Streams.streamOf(AccountBody.class).getType();
        final StreamConverter<AccountBody> unit =
                new StreamConverter<>(new ObjectMapper().findAndRegisterModules(),
                        singletonList(APPLICATION_X_JSON_STREAM));
        final HttpInputMessage input = mockWithContentType(APPLICATION_X_JSON_STREAM);
        when(input.getBody()).thenReturn(new ClassPathResource("account-stream.json").getInputStream());

        final Stream<AccountBody> stream = unit.read(type, null, input);

        @SuppressWarnings("unchecked") final Consumer<? super AccountBody> verifier = mock(Consumer.class);

        stream.forEach(verifier);

        verify(verifier).accept(new AccountBody("1234567890", "Acme Corporation"));
        verify(verifier).accept(new AccountBody("1234567891", "Acme Company"));
        verify(verifier).accept(new AccountBody("1234567892", "Acme GmbH"));
        verify(verifier).accept(new AccountBody("1234567893", "Acme SE"));
        verify(verifier, times(4)).accept(any(AccountBody.class));
    }

    @Test
    void shouldSupportReadSequence() throws Exception {
        final Type type = Streams.streamOf(AccountBody.class).getType();
        final StreamConverter<AccountBody> unit =
                new StreamConverter<>(new ObjectMapper().findAndRegisterModules(),
                        singletonList(APPLICATION_JSON_SEQ));
        final HttpInputMessage input = mockWithContentType(APPLICATION_JSON_SEQ);
        when(input.getBody()).thenReturn(new ClassPathResource("account-sequence.json").getInputStream());

        final Stream<AccountBody> stream = unit.read(type, null, input);

        @SuppressWarnings("unchecked") final Consumer<? super AccountBody> verifier = mock(Consumer.class);

        stream.forEach(verifier);

        verify(verifier).accept(new AccountBody("1234567890", "Acme Corporation"));
        verify(verifier).accept(new AccountBody("1234567891", "Acme Company"));
        verify(verifier).accept(new AccountBody("1234567892", "Acme GmbH"));
        verify(verifier).accept(new AccountBody("1234567893", "Acme SE"));
        verify(verifier, times(4)).accept(any(AccountBody.class));
    }

    @Test
    void shouldThrowIOExceptionOnClose() throws Exception {
        final ObjectMapper mapper = spy(new ObjectMapper().findAndRegisterModules());
        final JsonFactory factory = spy(mapper.getFactory());
        final JsonParser parser = mock(JsonParser.class);
        when(mapper.getFactory()).thenReturn(factory);
        when(factory.createParser(any(InputStream.class))).thenReturn(parser);
        doThrow(new IOException()).when(parser).close();

        final Type type = Streams.streamOf(AccountBody.class).getType();
        final StreamConverter<AccountBody> unit = streamConverter(mapper);
        final HttpInputMessage input = mockWithContentType(APPLICATION_X_JSON_STREAM);
        when(input.getBody()).thenReturn(new ClassPathResource("account-stream.json").getInputStream());

        assertThrows(IOException.class, () -> unit.read(type, null, input).close());
    }

    @Test
    void shouldFailOnReadForIOException() throws Exception {
        final StreamConverter<Object> unit = streamConverter();
        final HttpInputMessage input = mockWithContentType(APPLICATION_X_JSON_STREAM);

        doThrow(new IOException()).when(input).getBody();

        final HttpMessageNotReadableException exception = assertThrows(HttpMessageNotReadableException.class, () ->
                unit.read(Streams.streamOf(Object.class).getType(), null, input));

        assertThat(exception.getCause(), is(instanceOf(IOException.class)));
    }

    @Test
    void writeNotSupported() {
        final HttpMessageConverter<Stream<Object>> unit = streamConverter();

        assertThrows(UnsupportedOperationException.class, () ->
                unit.write(null, APPLICATION_X_JSON_STREAM, null));
    }

    @Test
    void writeGenericNotSupported() {
        final StreamConverter<AccountBody> unit = streamConverter();

        assertThrows(UnsupportedOperationException.class, () ->
                unit.write(null, Streams.streamOf(AccountBody.class).getType(), APPLICATION_X_JSON_STREAM, null));
    }
}

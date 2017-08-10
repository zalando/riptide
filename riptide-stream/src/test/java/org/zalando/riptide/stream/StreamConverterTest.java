package org.zalando.riptide.stream;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_XML;
import static org.zalando.riptide.stream.Streams.APPLICATION_JSON_SEQ;
import static org.zalando.riptide.stream.Streams.APPLICATION_X_JSON_STREAM;
import static org.zalando.riptide.stream.Streams.streamConverter;

public class StreamConverterTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void shouldSupportMediaTypes() throws Exception {
        final List<MediaType> medias = streamConverter().getSupportedMediaTypes();
        assertThat(medias, hasItem(APPLICATION_X_JSON_STREAM));
        assertThat(medias, hasItem(APPLICATION_JSON_SEQ));
    }

    @Test
    public void shouldSupportRead() throws Exception {
        final HttpMessageConverter<Stream<Object>> unit = streamConverter(new ObjectMapper(), singletonList(APPLICATION_JSON));

        assertFalse(unit.canRead(Object.class, APPLICATION_XML));
        assertFalse(unit.canRead(Stream.class, APPLICATION_JSON));
        assertFalse(unit.canRead(Object.class, APPLICATION_JSON));
        assertFalse(unit.canRead(List.class, APPLICATION_JSON));
        assertFalse(unit.canRead(List[].class, APPLICATION_JSON));
        assertFalse(unit.canRead(AccountBody.class, null));
        assertFalse(unit.canRead(AccountBody[].class, null));
    }

    @Test
    public void shouldSupportReadGeneric() throws Exception {
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
    public void shouldNotSupportWrite() throws Exception {
        final HttpMessageConverter<Stream<AccountBody>> unit = streamConverter();
        assertFalse(unit.canWrite(AccountBody.class, APPLICATION_X_JSON_STREAM));
    }

    @Test
    public void shouldNotSupportWriteGeneric() throws Exception {
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
    @Test(expected = UnsupportedOperationException.class)
    public void shouldNotSupportReadStream() throws Exception {
        final StreamConverter unit =
                new StreamConverter<>(new ObjectMapper().findAndRegisterModules(),
                        singletonList(APPLICATION_X_JSON_STREAM));
        final HttpInputMessage input = mockWithContentType(APPLICATION_X_JSON_STREAM);
        when(input.getBody()).thenReturn(new ClassPathResource("account-stream.json").getInputStream());

        unit.read(Stream.class, input);
    }

    @Test
    public void shouldSupportGenericReadStream() throws Exception {
        final Type type = Streams.streamOf(AccountBody.class).getType();
        final StreamConverter<AccountBody> unit =
                new StreamConverter<>(new ObjectMapper().findAndRegisterModules(),
                        singletonList(APPLICATION_X_JSON_STREAM));
        final HttpInputMessage input = mockWithContentType(APPLICATION_X_JSON_STREAM);
        when(input.getBody()).thenReturn(new ClassPathResource("account-stream.json").getInputStream());

        final Stream<AccountBody> stream = unit.read(type, null, input);

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
        final Type type = Streams.streamOf(AccountBody.class).getType();
        final StreamConverter<AccountBody> unit =
                new StreamConverter<>(new ObjectMapper().findAndRegisterModules(),
                        singletonList(APPLICATION_JSON_SEQ));
        final HttpInputMessage input = mockWithContentType(APPLICATION_JSON_SEQ);
        when(input.getBody()).thenReturn(new ClassPathResource("account-sequence.json").getInputStream());

        final Stream<AccountBody> stream = unit.read(type, null, input);

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
    public void shouldThrowIOExceptionOnClose() throws Exception {
        exception.expect(IOException.class);

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

        try (Stream<AccountBody> stream = unit.read(type, null, input)) {
            // nothing to do.
            stream.close();
        }
    }

    @Test
    public void shouldFailOnReadForIOException() throws Exception {
        exception.expect(HttpMessageNotReadableException.class);
        exception.expectCause(instanceOf(IOException.class));

        final StreamConverter<Object> unit = streamConverter();
        final HttpInputMessage input = mockWithContentType(APPLICATION_X_JSON_STREAM);

        doThrow(new IOException()).when(input).getBody();

        unit.read(Streams.streamOf(Object.class).getType(), null, input);
    }

    @Test
    public void writeNotSupported() throws Exception {
        exception.expect(UnsupportedOperationException.class);

        final HttpMessageConverter<Stream<Object>> unit = streamConverter();
        unit.write(null, APPLICATION_X_JSON_STREAM, null);
    }

    @Test
    public void writeGenericNotSupported() throws Exception {
        exception.expect(UnsupportedOperationException.class);

        final StreamConverter<AccountBody> unit = streamConverter();
        unit.write(null, Streams.streamOf(AccountBody.class).getType(), APPLICATION_X_JSON_STREAM, null);
    }
}

package org.zalando.riptide;

import com.google.common.reflect.TypeToken;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.io.InputStream;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.zalando.fauxpas.FauxPas.throwingRunnable;

final class InputStreamTest {

    private static final class StreamConverter<T> implements HttpMessageConverter<Stream<T>> {

        @Override
        public boolean canRead(final Class<?> clazz, final MediaType mediaType) {
            return clazz == Stream.class;
        }

        @Override
        public boolean canWrite(final Class<?> clazz, final MediaType mediaType) {
            return false;
        }

        @Override
        public List<MediaType> getSupportedMediaTypes() {
            return Collections.emptyList();
        }

        @Override
        public Stream<T> read(final Class<? extends Stream<T>> clazz,
                final HttpInputMessage inputMessage) {

            return Stream.<T>empty().onClose(throwingRunnable(() ->
                    inputMessage.getBody().close()));
        }

        @Override
        public void write(final Stream<T> tStream, final MediaType contentType,
                final HttpOutputMessage outputMessage)  {
            throw new UnsupportedOperationException();
        }
    }

    private final URI url = URI.create("https://api.example.com/blobs/123");

    private final Http unit;
    private final MockRestServiceServer server;

    InputStreamTest() {
        final RestTemplate template = new RestTemplate();
        this.server = MockRestServiceServer.createServer(template);
        this.unit = Http.builder()
                .executor(Executors.newSingleThreadExecutor())
                .requestFactory(template.getRequestFactory())
                .baseUrl("https://api.example.com")
                .converter(new StreamConverter<>())
                .build();
    }

    @Test
    void wontCloseBodyForAutoCloseableBodyTypes() {
        server.expect(requestTo(url)).andRespond(
                withSuccess()
                        .body(new byte[]{'b', 'l', 'o', 'b'}));

        unit.get(url).call((response, reader) -> {
            final TypeToken<Stream> type = TypeToken.of(Stream.class);
            final Stream<?> stream = reader.read(type, response);

            final InputStream body = response.getBody();
            assertThat(body.available(), is(greaterThan(0)));
            stream.close();

            assertThat(body.available(), is(0));
        });
    }

}

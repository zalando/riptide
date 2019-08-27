package org.zalando.riptide;

import org.junit.jupiter.api.*;
import org.springframework.core.io.*;
import org.springframework.http.*;
import org.springframework.http.converter.*;
import org.springframework.test.web.client.*;
import org.springframework.web.client.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import static java.util.Collections.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.MediaType.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import static org.zalando.riptide.Bindings.*;
import static org.zalando.riptide.Navigators.*;

final class InputStreamTest {

    static class InputStreamHttpMessageConverter implements HttpMessageConverter<InputStream> {

        @Override
        public boolean canRead(final Class<?> clazz, final MediaType mediaType) {
            return InputStream.class.isAssignableFrom(clazz);
        }

        @Override
        public boolean canWrite(final Class<?> clazz, final MediaType mediaType) {
            return false;
        }

        @Override
        public List<MediaType> getSupportedMediaTypes() {
            return singletonList(APPLICATION_OCTET_STREAM);
        }

        @Override
        public InputStream read(final Class<? extends InputStream> clazz, final HttpInputMessage inputMessage) throws IOException,
                HttpMessageNotReadableException {
            return inputMessage.getBody();
        }

        @Override
        public void write(final InputStream t, final MediaType contentType, final HttpOutputMessage outputMessage) throws
                HttpMessageNotWritableException {
            throw new IllegalStateException();
        }

    }

    static class CloseOnceInputStream extends InputStream {
        private final InputStream inputStream;
        private boolean isClosed;

        CloseOnceInputStream(final InputStream inputStream) {
            this.inputStream = inputStream;
        }

        CloseOnceInputStream(final byte[] buf) {
            this(new ByteArrayInputStream(buf));
        }

        private void checkClosed() throws IOException {
            if (isClosed) {
                throw new IOException("Stream is already closed");
            }
        }

        @Override
        public void close() throws IOException {
            checkClosed();
            isClosed = true;
            inputStream.close();
        }

        @Override
        public synchronized void mark(final int readlimit) {
            inputStream.mark(readlimit);
        }

        @Override
        public synchronized void reset() throws IOException {
            checkClosed();
            inputStream.reset();
        }

        @Override
        public boolean markSupported() {
            return inputStream.markSupported();
        }

        @Override
        public synchronized int read() throws IOException {
            checkClosed();
            return inputStream.read();
        }

        @Override
        public synchronized int read(final byte[] b, final int off, final int len) throws IOException {
            checkClosed();
            return inputStream.read(b, off, len);
        }

        @Override
        public synchronized long skip(final long n) throws IOException {
            checkClosed();
            return inputStream.skip(n);
        }

        @Override
        public synchronized int available() throws IOException {
            checkClosed();
            return inputStream.available();
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
                .converter(new InputStreamHttpMessageConverter())
                .baseUrl("https://api.example.com")
                .build();
    }

    @Test
    void shouldAllowCloseOnce() throws IOException {
        final InputStream content = new CloseOnceInputStream(new byte[]{'b', 'l', 'o', 'b'});
        content.close();
        try {
            content.close();
            fail("Should prevent multiple close calls");
        } catch (final IOException e) {
            assertEquals("Stream is already closed", e.getMessage());
        }
    }

    @Test
    void shouldNotAllowReadAfterClose() throws IOException {
        final InputStream content = new CloseOnceInputStream(new byte[]{'b', 'l', 'o', 'b'});
        content.close();
        try {
            //noinspection ResultOfMethodCallIgnored
            content.read();
            fail("Should prevent read calls after close");
        } catch (final IOException e) {
            assertEquals("Stream is already closed", e.getMessage());
        }
    }

    @Test
    void shouldExtractOriginalBody() throws IOException {
        final InputStream content = new CloseOnceInputStream(new byte[]{'b', 'l', 'o', 'b'});

        server.expect(requestTo(url)).andRespond(
                withSuccess()
                        .body(new InputStreamResource(content))
                        .contentType(APPLICATION_OCTET_STREAM));

        final AtomicReference<InputStream> capture = new AtomicReference<>();

        unit.get(url)
                .dispatch(contentType(),
                        on(APPLICATION_OCTET_STREAM).call(InputStream.class, capture::set))
                .join();

        final InputStream inputStream = capture.get();

        assertEquals(content, inputStream);

        final int ch1 = inputStream.read();
        assertEquals('b', ch1);
    }

}

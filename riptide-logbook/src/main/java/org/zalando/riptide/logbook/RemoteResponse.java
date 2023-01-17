package org.zalando.riptide.logbook;

import com.google.common.io.ByteStreams;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.zalando.logbook.HttpResponse;
import org.zalando.logbook.Origin;
import org.zalando.riptide.CharsetExtractor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.zalando.fauxpas.FauxPas.throwingFunction;
import static org.zalando.fauxpas.FauxPas.throwingUnaryOperator;

@AllArgsConstructor
final class RemoteResponse implements HttpResponse {

    private static final CharsetExtractor EXTRACTOR = new CharsetExtractor();

    private final AtomicReference<State> state = new AtomicReference<>(new Unbuffered());

    private final ClientHttpResponse response;

    private interface State {

        default State with() {
            return this;
        }

        default State without() {
            return this;
        }

        default State buffer() throws IOException {
            return this;
        }

        default InputStream getBody(final InputStream original) {
            return original;
        }

        default byte[] getBufferedBody() {
            return new byte[0];
        }

    }

    private final class Unbuffered implements State {

        @Override
        public State with() {
            return new Offering();
        }

        @Override
        public State buffer() {
            return new Passing();
        }

    }

    private final class Offering implements State {

        @Override
        public State without() {
            return new Unbuffered();
        }

        @Override
        public State buffer() throws IOException {
            return new Buffering();
        }

    }

    @AllArgsConstructor
    private final class Buffering implements State {

        private byte[] body;

        private Buffering() throws IOException {
            this(ByteStreams.toByteArray(response.getBody()));
        }

        @Override
        public State without() {
            return new Ignoring(body);
        }

        @Override
        public InputStream getBody(final InputStream original) {
            return new ByteArrayInputStream(body);
        }

        @Override
        public byte[] getBufferedBody() {
            return body;
        }

    }

    @AllArgsConstructor
    private final class Ignoring implements State {

        private final byte[] body;

        @Override
        public State with() {
            return new Buffering(body);
        }

    }

    private static final class Passing implements State {

    }

    @Override
    public HttpResponse withBody() {
        state.updateAndGet(throwingUnaryOperator(State::with));
        return this;
    }

    @Override
    public HttpResponse withoutBody() {
        state.updateAndGet(State::without);
        return this;
    }

    @Override
    public Origin getOrigin() {
        return Origin.REMOTE;
    }

    @Override
    public String getProtocolVersion() {
        return "HTTP/1.1"; // TODO
    }

    @Override
    public int getStatus() {
        return throwingFunction(ClientHttpResponse::getRawStatusCode).apply(response);
    }

    @Override
    public org.zalando.logbook.HttpHeaders getHeaders() {
        return org.zalando.logbook.HttpHeaders.of(response.getHeaders());
    }

    @Nullable
    @Override
    public String getContentType() {
        return Optional.ofNullable(response.getHeaders().getContentType())
                .map(Objects::toString)
                .orElse(null);
    }

    @Override
    public Charset getCharset() {
        return Optional.ofNullable(response.getHeaders().getContentType())
                .flatMap(EXTRACTOR::getCharset)
                .orElse(StandardCharsets.UTF_8);
    }

    @Override
    public byte[] getBody() {
        return state.updateAndGet(throwingUnaryOperator(State::buffer))
                .getBufferedBody();
    }

    ClientHttpResponse asClientHttpResponse() {
        return new ClientHttpResponseAdapter();
    }

    private final class ClientHttpResponseAdapter implements ClientHttpResponse {

        @Nonnull
        @Override
        public HttpStatusCode getStatusCode() throws IOException {
            return response.getStatusCode();
        }

        @Override
        public int getRawStatusCode() throws IOException {
            return response.getRawStatusCode();
        }

        @Nonnull
        @Override
        public String getStatusText() throws IOException {
            return response.getStatusText();
        }

        @Override
        public void close() {
            response.close();
        }

        @Nonnull
        @Override
        public InputStream getBody() throws IOException {
            return state.get().getBody(response.getBody());
        }

        @Nonnull
        @Override
        public HttpHeaders getHeaders() {
            return response.getHeaders();
        }

    }

}

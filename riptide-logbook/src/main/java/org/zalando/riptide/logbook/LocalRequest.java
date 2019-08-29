package org.zalando.riptide.logbook;

import com.google.common.collect.Iterables;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.zalando.logbook.HttpRequest;
import org.zalando.logbook.Origin;
import org.zalando.riptide.CharsetExtractor;
import org.zalando.riptide.RequestArguments;
import org.zalando.riptide.RequestArguments.Entity;

import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static com.google.common.base.MoreObjects.firstNonNull;
import static java.util.Collections.emptyList;
import static org.zalando.fauxpas.FauxPas.throwingUnaryOperator;

@AllArgsConstructor
final class LocalRequest implements HttpRequest, Entity {

    private static final CharsetExtractor EXTRACTOR = new CharsetExtractor();

    private final AtomicReference<State> state = new AtomicReference<>(new Unbuffered());

    private final RequestArguments arguments;

    private interface State {

        default State with() {
            return this;
        }

        default State without() {
            return this;
        }

        default State buffer(final Entity entity, final HttpOutputMessage message) throws IOException {
            throw new UnsupportedOperationException();
        }

        default byte[] getBody() {
            return new byte[0];
        }

    }

    private static final class Unbuffered implements State {

        @Override
        public State with() {
            return new Offering();
        }

        @Override
        public State buffer(final Entity entity, final HttpOutputMessage message) throws IOException {
            entity.writeTo(message);
            return new Passing();
        }

    }

    private static final class Offering implements State {

        @Override
        public State without() {
            return new Unbuffered();
        }

        @Override
        public State buffer(final Entity entity, final HttpOutputMessage message) throws IOException {
            final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            entity.writeTo(new TeeHttpOutputMessage(message, buffer));
            return new Buffering(buffer.toByteArray());
        }

    }

    @AllArgsConstructor
    private static final class Buffering implements State {

        private byte[] body;

        @Override
        public State without() {
            return new Ignoring(body);
        }

        @Override
        public byte[] getBody() {
            return body;
        }

    }

    @AllArgsConstructor
    private static final class Ignoring implements State {

        private final byte[] body;

        @Override
        public State with() {
            return new Buffering(body);
        }

    }

    private static final class Passing implements State {

    }

    @Override
    public HttpRequest withBody() {
        state.updateAndGet(State::with);
        return this;
    }

    @Override
    public HttpRequest withoutBody() {
        state.updateAndGet(State::without);
        return this;
    }

    @Override
    public Origin getOrigin() {
        return Origin.LOCAL;
    }

    @Override
    public String getProtocolVersion() {
        return "HTTP/1.1";
    }

    @Override
    public String getRemote() {
        return "localhost";
    }

    @Override
    public String getMethod() {
        return arguments.getMethod().name();
    }

    @Override
    public String getScheme() {
        return arguments.getRequestUri().getScheme();
    }

    @Override
    public String getHost() {
        return arguments.getRequestUri().getHost();
    }

    @Override
    public Optional<Integer> getPort() {
        return Optional.of(arguments.getRequestUri().getPort()).filter(p -> p != -1);
    }

    @Override
    public String getPath() {
        return arguments.getRequestUri().getPath();
    }

    @Override
    public String getQuery() {
        return Optional.ofNullable(arguments.getRequestUri().getQuery()).orElse("");
    }

    @Override
    public Map<String, List<String>> getHeaders() {
        return arguments.getHeaders();
    }

    @Nullable
    @Override
    public String getContentType() {
        return Iterables.getFirst(
                firstNonNull(arguments.getHeaders().get("Content-Type"), emptyList()), null);
    }

    @Override
    public Charset getCharset() {
        return Optional.ofNullable(getContentType())
                .map(MediaType::parseMediaType)
                .flatMap(EXTRACTOR::getCharset)
                .orElse(StandardCharsets.UTF_8);
    }

    @Override
    public void writeTo(final HttpOutputMessage message) {
        state.updateAndGet(throwingUnaryOperator(state ->
                state.buffer(arguments.getEntity(), message)));
    }

    @Override
    public byte[] getBody() {
        return state.get().getBody();
    }

}

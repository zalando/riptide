package org.zalando.riptide;

import com.google.common.base.*;
import com.google.common.collect.*;
import lombok.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import org.springframework.http.*;
import org.springframework.http.client.*;
import org.springframework.test.web.client.*;

import javax.annotation.*;
import java.net.*;
import java.util.Objects;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.stream.*;

import static java.util.EnumSet.allOf;
import static java.util.stream.Collectors.*;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import static org.zalando.riptide.PassRoute.*;
import static org.zalando.riptide.UrlResolution.*;

final class RequestUriTest {

    static List<Arguments> data() {
        final Set<Arguments> methods = allOf(HttpMethod.class).stream()
                .map(Arguments::of)
                .collect(toSet());

        return Sets.cartesianProduct(getCases(), methods).stream()
                .map(arguments -> arguments.stream().map(Arguments::get)
                        .reduce((l, r) -> ObjectArrays.concat(l, r, Object.class))
                        .map(Arguments::of)
                        .orElseThrow(IllegalArgumentException::new))
                .collect(toList());
    }

    private static Set<Arguments> getCases() {
        return ImmutableSet.of(
                Arguments.of("https://example.com", RFC, null, uri("https://example.com")),
                Arguments.of("https://example.com/", RFC, null, uri("https://example.com/")),
                Arguments.of("https://example.com", RFC, "", uri("https://example.com")),
                Arguments.of("https://example.com/", RFC, "", uri("https://example.com/")),
                Arguments.of("https://example.com", RFC, "/", uri("https://example.com/")),
                Arguments.of("https://example.com/", RFC, "/", uri("https://example.com/")),
                Arguments.of("https://example.com", RFC, "https://example.org/foo", uri("https://example.org/foo")),
                Arguments.of("https://example.com", RFC, "/foo/bar", uri("https://example.com/foo/bar")),
                Arguments.of("https://example.com", RFC, "foo/bar", uri("https://example.com/foo/bar")),
                Arguments.of("https://example.com/api", RFC, "/foo/bar", uri("https://example.com/foo/bar")),
                Arguments.of("https://example.com/api", RFC, "foo/bar", uri("https://example.com/foo/bar")),
                Arguments.of("https://example.com/api/", RFC, "/foo/bar", uri("https://example.com/foo/bar")),
                Arguments.of("https://example.com/api/", RFC, "foo/bar", uri("https://example.com/api/foo/bar")),
                Arguments.of(null, RFC, "https://example.com/foo", uri("https://example.com/foo")),
                Arguments.of("/foo", RFC, "/", error("Base URL is not absolute")),
                Arguments.of(null, RFC, null, error("Either Base URL or absolute Request URI is required")),
                Arguments.of(null, RFC, "/foo", error("Request URI is not absolute")),
                Arguments.of(null, RFC, "foo", error("Request URI is not absolute")),
                Arguments.of("https://example.com", APPEND, null, uri("https://example.com")),
                Arguments.of("https://example.com/", APPEND, null, uri("https://example.com/")),
                Arguments.of("https://example.com", APPEND, "", uri("https://example.com")),
                Arguments.of("https://example.com/", APPEND, "", uri("https://example.com/")),
                Arguments.of("https://example.com", APPEND, "/", uri("https://example.com/")),
                Arguments.of("https://example.com/", APPEND, "/", uri("https://example.com/")),
                Arguments.of("https://example.com", APPEND, "https://example.org/foo", uri("https://example.org/foo")),
                Arguments.of("https://example.com", APPEND, "/foo/bar", uri("https://example.com/foo/bar")),
                Arguments.of("https://example.com", APPEND, "foo/bar", uri("https://example.com/foo/bar")),
                Arguments.of("https://example.com/api", APPEND, "/foo/bar", uri("https://example.com/api/foo/bar")),
                Arguments.of("https://example.com/api", APPEND, "foo/bar", uri("https://example.com/api/foo/bar")),
                Arguments.of("https://example.com/api/", APPEND, "/foo/bar", uri("https://example.com/api/foo/bar")),
                Arguments.of("https://example.com/api/", APPEND, "foo/bar", uri("https://example.com/api/foo/bar")),
                Arguments.of(null, APPEND, "https://example.com/foo", uri("https://example.com/foo")),
                Arguments.of("/foo", APPEND, "/", error("Base URL is not absolute")),
                Arguments.of(null, APPEND, null, error("Either Base URL or absolute Request URI is required")),
                Arguments.of(null, APPEND, "/foo", error("Request URI is not absolute")),
                Arguments.of(null, APPEND, "foo", error("Request URI is not absolute")));
    }

    private interface Result {
        void execute(final String baseUrl, final UrlResolution resolution, @Nullable final String uri,
                final HttpMethod method, final Consumer<Http> tester);
    }

    @Value
    private static final class RequestUri implements Result {

        String requestUri;

        @Override
        public void execute(final String baseUrl, final UrlResolution resolution, @Nullable final String uri,
                final HttpMethod method, @Nonnull final Consumer<Http> tester) {

            final MockSetup setup = new MockSetup(baseUrl);
            final Http unit = setup.getHttpBuilder().urlResolution(resolution).build();
            final MockRestServiceServer server = setup.getServer();

            server.expect(requestTo(requestUri))
                    .andExpect(method(method))
                    .andRespond(withSuccess());

            tester.accept(unit);
            server.verify();

        }

        @Override
        public String toString() {
            return requestUri;
        }

    }

    private static Result uri(final String uri) {
        return new RequestUri(uri);
    }

    @Value
    private static final class Failure implements Result {

        String message;

        @Override
        public void execute(final String baseUrl, final UrlResolution resolution, @Nullable final String uri,
                final HttpMethod method, final Consumer<Http> tester) {

            try {
                final Http unit = Http.builder()
                        .executor(Executors.newSingleThreadExecutor())
                        .requestFactory(new SimpleClientHttpRequestFactory())
                        .baseUrl(baseUrl)
                        .urlResolution(resolution)
                        .build();

                tester.accept(unit);
                fail("Expected exception");
            } catch (final Exception e) {
                final Throwable cause = Throwables.getRootCause(e);
                assertThat(cause, is(instanceOf(IllegalArgumentException.class)));
                assertThat(cause.getMessage(), is(message));
            }
        }

        @Override
        public String toString() {
            return "Exception";
        }

    }

    private static Result error(final String message) {
        return new Failure(message);
    }

    @ParameterizedTest
    @MethodSource("data")
    void shouldIgnoreEmptyUri(final String baseUrl, final UrlResolution resolution, @Nullable final String uri,
            final Result result, final HttpMethod method) {
        assumeTrue(uri == null);

        result.execute(baseUrl, resolution, null, method, http ->
                http.execute(method).call(pass()).join());
    }

    @ParameterizedTest
    @MethodSource("data")
    void shouldResolveUri(final String baseUrl, final UrlResolution resolution, @Nullable final String uri,
            final Result result, final HttpMethod method) {
        assumeTrue(uri != null);

        result.execute(baseUrl, resolution, uri, method, http ->
                http.execute(method, URI.create(uri)).call(pass()).join());
    }

    @ParameterizedTest
    @MethodSource("data")
    void shouldResolveUriTemplate(final String baseUrl, final UrlResolution resolution, @Nullable final String uri,
            final Result result, final HttpMethod method) {
        assumeTrue(uri != null);

        result.execute(baseUrl, resolution, uri, method, http ->
                http.execute(method, uri).call(pass()).join());
    }

    /**
     * Used to re-generate the URI example table in the {@code README.md}.
     *
     * @param args ignored
     */
    static void main(final String[] args) {
        getCases().stream()
                .map(test -> {
                    final Stream<String> row = Stream.of(test.get()).map(String::valueOf);

                    return row
                            .map(Objects::toString)
                            .map(cell -> {
                                switch (cell) {
                                    case "":
                                        return "(empty)";
                                    case "Exception":
                                        return cell;
                                    default:
                                        return "`" + cell + "`";
                                }
                            }).collect(joining("|", "|", "|"));
                })
                .forEach(System.out::println);
    }

}

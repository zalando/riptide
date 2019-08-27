package org.zalando.riptide;

import lombok.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import org.springframework.http.*;
import org.springframework.test.web.client.*;

import java.net.*;
import java.util.*;
import java.util.function.*;

import static org.springframework.http.HttpMethod.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import static org.zalando.riptide.PassRoute.*;

/**
 * A test that verifies that the specific request methods in {@link Http}, e.g. {@link Http#get(URI)} delegate the
 * chosen method to {@link Http#execute(HttpMethod, URI)} correctly
 *
 * @see Http#get()
 * @see Http#execute(HttpMethod)
 * @see Http#get(URI)
 * @see Http#execute(HttpMethod, URI)
 * @see Http#get(String, Object...)
 * @see Http#execute(HttpMethod, String, Object...)
 */
final class MethodDelegateTest {

    static List<Arguments> data() {
        return Arrays.asList(
                Arguments.of(GET, new NoParam(Http::get)),
                Arguments.of(HEAD, new NoParam(Http::head)),
                Arguments.of(POST, new NoParam(Http::post)),
                Arguments.of(PUT, new NoParam(Http::put)),
                Arguments.of(PATCH, new NoParam(Http::patch)),
                Arguments.of(DELETE, new NoParam(Http::delete)),
                Arguments.of(OPTIONS, new NoParam(Http::options)),
                Arguments.of(TRACE, new NoParam(Http::trace)),
                Arguments.of(GET, new UriParam(Http::get, URI.create("https://example.com"))),
                Arguments.of(HEAD, new UriParam(Http::head, URI.create("https://example.com"))),
                Arguments.of(POST, new UriParam(Http::post, URI.create("https://example.com"))),
                Arguments.of(PUT, new UriParam(Http::put, URI.create("https://example.com"))),
                Arguments.of(PATCH, new UriParam(Http::patch, URI.create("https://example.com"))),
                Arguments.of(DELETE, new UriParam(Http::delete, URI.create("https://example.com"))),
                Arguments.of(OPTIONS, new UriParam(Http::options, URI.create("https://example.com"))),
                Arguments.of(TRACE, new UriParam(Http::trace, URI.create("https://example.com"))),
                Arguments.of(GET, new UriTemplateParam(Http::get, "https://example.com")),
                Arguments.of(HEAD, new UriTemplateParam(Http::head, "https://example.com")),
                Arguments.of(POST, new UriTemplateParam(Http::post, "https://example.com")),
                Arguments.of(PUT, new UriTemplateParam(Http::put, "https://example.com")),
                Arguments.of(PATCH, new UriTemplateParam(Http::patch, "https://example.com")),
                Arguments.of(DELETE, new UriTemplateParam(Http::delete, "https://example.com")),
                Arguments.of(OPTIONS, new UriTemplateParam(Http::options, "https://example.com")),
                Arguments.of(TRACE, new UriTemplateParam(Http::trace, "https://example.com"))
        );
    }

    private interface Tester {
        HeaderStage test(final Http unit);
    }

    @Value
    private static final class NoParam implements Tester {
        Function<Http, HeaderStage> function;

        @Override
        public HeaderStage test(final Http unit) {
            return function.apply(unit);
        }

        @Override
        public String toString() {
            return "No URI";
        }
    }

    @Value
    private static final class UriParam implements Tester {
        BiFunction<Http, URI, HeaderStage> function;
        URI parameter;

        @Override
        public HeaderStage test(final Http unit) {
            return function.apply(unit, parameter);
        }

        @Override
        public String toString() {
            return "URI";
        }
    }

    @Value
    private static final class UriTemplateParam implements Tester {
        TriFunction<Http, String, Object[], HeaderStage> function;
        String parameter;

        @Override
        public HeaderStage test(final Http unit) {
            return function.apply(unit, parameter, new Object[0]);
        }

        @Override
        public String toString() {
            return "URI Template";
        }
    }

    @FunctionalInterface
    interface TriFunction<T, U, V, R> {
        R apply(T t, U u, V v);
    }

    @ParameterizedTest
    @MethodSource("data")
    void shouldDelegate(final HttpMethod method, final Tester tester) {
        final MockSetup setup = new MockSetup("https://example.com");
        final Http unit = setup.getHttp();
        final MockRestServiceServer server = setup.getServer();

        server.expect(requestTo("https://example.com"))
                .andExpect(method(method))
                .andRespond(withSuccess());

        tester.test(unit)
                .call(pass())
                .join();

        server.verify();
    }

}

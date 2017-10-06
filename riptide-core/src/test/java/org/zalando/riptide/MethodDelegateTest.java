package org.zalando.riptide;

import lombok.Value;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;

import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.BiFunction;
import java.util.function.Function;

import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.HEAD;
import static org.springframework.http.HttpMethod.OPTIONS;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.http.HttpMethod.TRACE;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.zalando.riptide.PassRoute.pass;

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
@RunWith(Parameterized.class)
public final class MethodDelegateTest {

    private final HttpMethod method;
    private final Tester tester;

    public MethodDelegateTest(final HttpMethod method, final Tester tester) {
        this.method = method;
        this.tester = tester;
    }

    @Parameterized.Parameters(name = "{0} {1}")
    @SuppressWarnings("unchecked")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                {GET, new NoParam(Http::get)},
                {HEAD, new NoParam(Http::head)},
                {POST, new NoParam(Http::post)},
                {PUT, new NoParam(Http::put)},
                {PATCH, new NoParam(Http::patch)},
                {DELETE, new NoParam(Http::delete)},
                {OPTIONS, new NoParam(Http::options)},
                {TRACE, new NoParam(Http::trace)},
                {GET, new UriParam(Http::get, URI.create("https://example.com"))},
                {HEAD, new UriParam(Http::head, URI.create("https://example.com"))},
                {POST, new UriParam(Http::post, URI.create("https://example.com"))},
                {PUT, new UriParam(Http::put, URI.create("https://example.com"))},
                {PATCH, new UriParam(Http::patch, URI.create("https://example.com"))},
                {DELETE, new UriParam(Http::delete, URI.create("https://example.com"))},
                {OPTIONS, new UriParam(Http::options, URI.create("https://example.com"))},
                {TRACE, new UriParam(Http::trace, URI.create("https://example.com"))},
                {GET, new UriTemplateParam(Http::get, "https://example.com")},
                {HEAD, new UriTemplateParam(Http::head, "https://example.com")},
                {POST, new UriTemplateParam(Http::post, "https://example.com")},
                {PUT, new UriTemplateParam(Http::put, "https://example.com")},
                {PATCH, new UriTemplateParam(Http::patch, "https://example.com")},
                {DELETE, new UriTemplateParam(Http::delete, "https://example.com")},
                {OPTIONS, new UriTemplateParam(Http::options, "https://example.com")},
                {TRACE, new UriTemplateParam(Http::trace, "https://example.com")},
        });
    }

    private interface Tester {
        Requester test(final Http unit);
    }

    @Value
    private static final class NoParam implements Tester {
        Function<Http, Requester> function;

        @Override
        public Requester test(final Http unit) {
            return function.apply(unit);
        }

        @Override
        public String toString() {
            return "No URI";
        }
    }

    @Value
    private static final class UriParam implements Tester {
        BiFunction<Http, URI, Requester> function;
        URI parameter;

        @Override
        public Requester test(final Http unit) {
            return function.apply(unit, parameter);
        }

        @Override
        public String toString() {
            return "URI";
        }
    }

    @Value
    private static final class UriTemplateParam implements Tester {
        TriFunction<Http, String, Object[], Requester> function;
        String parameter;

        @Override
        public Requester test(final Http unit) {
            return function.apply(unit, parameter, new Object[0]);
        }

        @Override
        public String toString() {
            return "URI Template";
        }
    }

    @FunctionalInterface
    public interface TriFunction<T, U, V, R> {
        R apply(T t, U u, V v);
    }

    @Test
    public void shouldDelegate() {
        final MockSetup setup = new MockSetup("https://example.com");
        final Http unit = setup.getHttp();
        final MockRestServiceServer server = setup.getServer();

        server.expect(requestTo("https://example.com"))
                .andExpect(method(method))
                .andRespond(withSuccess());

        tester.test(unit)
                .call(pass());

        server.verify();
    }

}

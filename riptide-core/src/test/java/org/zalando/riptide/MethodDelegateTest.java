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
import static org.zalando.riptide.Route.call;
import static org.zalando.riptide.Route.pass;

/**
 * A test that verifies that the specific request methods in {@link Rest}, e.g. {@link Rest#get(URI)} delegate the
 * chosen method to {@link Rest#execute(HttpMethod, URI)} correctly
 *
 * @see Rest#get()
 * @see Rest#execute(HttpMethod)
 * @see Rest#get(URI)
 * @see Rest#execute(HttpMethod, URI)
 * @see Rest#get(String, Object...)
 * @see Rest#execute(HttpMethod, String, Object...)
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
                {GET, new NoParam(Rest::get)},
                {HEAD, new NoParam(Rest::head)},
                {POST, new NoParam(Rest::post)},
                {PUT, new NoParam(Rest::put)},
                {PATCH, new NoParam(Rest::patch)},
                {DELETE, new NoParam(Rest::delete)},
                {OPTIONS, new NoParam(Rest::options)},
                {TRACE, new NoParam(Rest::trace)},
                {GET, new UriParam(Rest::get, URI.create("https://example.com"))},
                {HEAD, new UriParam(Rest::head, URI.create("https://example.com"))},
                {POST, new UriParam(Rest::post, URI.create("https://example.com"))},
                {PUT, new UriParam(Rest::put, URI.create("https://example.com"))},
                {PATCH, new UriParam(Rest::patch, URI.create("https://example.com"))},
                {DELETE, new UriParam(Rest::delete, URI.create("https://example.com"))},
                {OPTIONS, new UriParam(Rest::options, URI.create("https://example.com"))},
                {TRACE, new UriParam(Rest::trace, URI.create("https://example.com"))},
                {GET, new UriTemplateParam(Rest::get, "https://example.com")},
                {HEAD, new UriTemplateParam(Rest::head, "https://example.com")},
                {POST, new UriTemplateParam(Rest::post, "https://example.com")},
                {PUT, new UriTemplateParam(Rest::put, "https://example.com")},
                {PATCH, new UriTemplateParam(Rest::patch, "https://example.com")},
                {DELETE, new UriTemplateParam(Rest::delete, "https://example.com")},
                {OPTIONS, new UriTemplateParam(Rest::options, "https://example.com")},
                {TRACE, new UriTemplateParam(Rest::trace, "https://example.com")},
        });
    }

    private interface Tester {
        Requester test(final Rest unit);
    }

    @Value
    private static final class NoParam implements Tester {
        Function<Rest, Requester> function;

        @Override
        public Requester test(final Rest unit) {
            return function.apply(unit);
        }

        @Override
        public String toString() {
            return "No URI";
        }
    }

    @Value
    private static final class UriParam implements Tester {
        BiFunction<Rest, URI, Requester> function;
        URI parameter;

        @Override
        public Requester test(final Rest unit) {
            return function.apply(unit, parameter);
        }

        @Override
        public String toString() {
            return "URI";
        }
    }

    @Value
    private static final class UriTemplateParam implements Tester {
        TriFunction<Rest, String, Object[], Requester> function;
        String parameter;

        @Override
        public Requester test(final Rest unit) {
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
        final Rest unit = setup.getRest();
        final MockRestServiceServer server = setup.getServer();

        server.expect(requestTo("https://example.com"))
                .andExpect(method(method))
                .andRespond(withSuccess());

        tester.test(unit)
                .call(call(pass()));

        server.verify();
    }

}

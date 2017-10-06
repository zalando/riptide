package org.zalando.riptide;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import lombok.Value;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.web.client.MockRestServiceServer;

import java.net.URI;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static java.util.EnumSet.allOf;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.zalando.riptide.PassRoute.pass;
import static org.zalando.riptide.UrlResolution.APPEND;
import static org.zalando.riptide.UrlResolution.RFC;

@RunWith(Parameterized.class)
public class RequestUriTest {

    private final String baseUrl;
    private final UrlResolution resolution;
    private final HttpMethod method;
    private final String uri;
    private final Result result;

    public RequestUriTest(final TestCase test, final HttpMethod method) {
        this.baseUrl = test.getBaseUrl();
        this.resolution = test.getResolution();
        this.method = method;
        this.uri = test.getUri();
        this.result = test.getResult();
    }

    @Parameters(name = "{1} {0}")
    @SuppressWarnings("unchecked")
    public static Collection<Object[]> data() {
        return Sets.cartesianProduct(getCases(), allOf(HttpMethod.class)).stream()
                .map(Collection::toArray)
                .collect(toList());
    }

    private static ImmutableSet<TestCase> getCases() {
        return ImmutableSet.of(
                new TestCase("https://example.com", RFC, null, uri("https://example.com")),
                new TestCase("https://example.com/", RFC, null, uri("https://example.com/")),
                new TestCase("https://example.com", RFC, "", uri("https://example.com")),
                new TestCase("https://example.com/", RFC, "", uri("https://example.com/")),
                new TestCase("https://example.com", RFC, "/", uri("https://example.com/")),
                new TestCase("https://example.com/", RFC, "/", uri("https://example.com/")),
                new TestCase("https://example.com", RFC, "https://example.org/foo", uri("https://example.org/foo")),
                new TestCase("https://example.com", RFC, "/foo/bar", uri("https://example.com/foo/bar")),
                new TestCase("https://example.com", RFC, "foo/bar", uri("https://example.com/foo/bar")),
                new TestCase("https://example.com/api", RFC, "/foo/bar", uri("https://example.com/foo/bar")),
                new TestCase("https://example.com/api", RFC, "foo/bar", uri("https://example.com/foo/bar")),
                new TestCase("https://example.com/api/", RFC, "/foo/bar", uri("https://example.com/foo/bar")),
                new TestCase("https://example.com/api/", RFC, "foo/bar", uri("https://example.com/api/foo/bar")),
                new TestCase(null, RFC, "https://example.com/foo", uri("https://example.com/foo")),
                new TestCase("/foo", RFC, "/", error("Base URL is not absolute")),
                new TestCase(null, RFC, null, error("Either Base URL or absolute Request URI is required")),
                new TestCase(null, RFC, "/foo", error("Request URI is not absolute")),
                new TestCase(null, RFC, "foo", error("Request URI is not absolute")),
                new TestCase("https://example.com", APPEND, null, uri("https://example.com")),
                new TestCase("https://example.com/", APPEND, null, uri("https://example.com/")),
                new TestCase("https://example.com", APPEND, "", uri("https://example.com")),
                new TestCase("https://example.com/", APPEND, "", uri("https://example.com/")),
                new TestCase("https://example.com", APPEND, "/", uri("https://example.com/")),
                new TestCase("https://example.com/", APPEND, "/", uri("https://example.com/")),
                new TestCase("https://example.com", APPEND, "https://example.org/foo", uri("https://example.org/foo")),
                new TestCase("https://example.com", APPEND, "/foo/bar", uri("https://example.com/foo/bar")),
                new TestCase("https://example.com", APPEND, "foo/bar", uri("https://example.com/foo/bar")),
                new TestCase("https://example.com/api", APPEND, "/foo/bar", uri("https://example.com/api/foo/bar")),
                new TestCase("https://example.com/api", APPEND, "foo/bar", uri("https://example.com/api/foo/bar")),
                new TestCase("https://example.com/api/", APPEND, "/foo/bar", uri("https://example.com/api/foo/bar")),
                new TestCase("https://example.com/api/", APPEND, "foo/bar", uri("https://example.com/api/foo/bar")),
                new TestCase(null, APPEND, "https://example.com/foo", uri("https://example.com/foo")),
                new TestCase("/foo", APPEND, "/", error("Base URL is not absolute")),
                new TestCase(null, APPEND, null, error("Either Base URL or absolute Request URI is required")),
                new TestCase(null, APPEND, "/foo", error("Request URI is not absolute")),
                new TestCase(null, APPEND, "foo", error("Request URI is not absolute")));
    }

    @Value
    private static final class TestCase {
        String baseUrl;
        UrlResolution resolution;
        String uri;
        Result result;

        @Override
        public String toString() {
            return MoreObjects.toStringHelper("")
                    .addValue(baseUrl)
                    .addValue(resolution)
                    .addValue(uri)
                    .addValue(result)
                    .toString();
        }
    }

    private interface Result {
        void execute(final String baseUrl, final UrlResolution resolution, final String uri, final HttpMethod method, final Consumer<Http> tester);
    }

    @Value
    private static final class RequestUri implements Result {

        String requestUri;

        @Override
        public void execute(final String baseUrl, final UrlResolution resolution, final String uri,
                final HttpMethod method, final Consumer<Http> tester) {

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
        public void execute(final String baseUrl, final UrlResolution resolution, final String uri,
                final HttpMethod method, final Consumer<Http> tester) {

            try {
                final Http unit = Http.builder()
                        .baseUrl(baseUrl)
                        .urlResolution(resolution)
                        .requestFactory(new SimpleClientHttpRequestFactory())
                        .build();

                tester.accept(unit);
                fail("Expected exception");
            } catch (final Exception e) {
                assertThat(e, is(instanceOf(IllegalArgumentException.class)));
                assertThat(e.getMessage(), is(message));
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

    @Test
    public void shouldIgnoreEmptyUri() {
        assumeThat(uri, is(nullValue()));

        result.execute(baseUrl, resolution, uri, method, http ->
                http.execute(method).call(pass()));
    }

    @Test
    public void shouldResolveUri() {
        assumeThat(uri, is(notNullValue()));

        result.execute(baseUrl, resolution, uri, method, http ->
                http.execute(method, URI.create(uri)).call(pass()));
    }

    @Test
    public void shouldResolveUriTemplate() {
        assumeThat(uri, is(notNullValue()));

        result.execute(baseUrl, resolution, uri, method, http ->
                http.execute(method, uri).call(pass()));
    }

    /**
     * Used to re-generate the URI example table in the {@code README.md}.
     *
     * @param args ignored
     */
    public static void main(final String[] args) {
        getCases().stream()
                .map(test -> {
                    final Stream<String> row = Stream.of(
                            test.getBaseUrl(),
                            test.getResolution().name(),
                            test.getUri(),
                            test.getResult().toString());

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

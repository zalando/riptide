package org.zalando.riptide.compatibility;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestOperations;
import org.zalando.riptide.Http;

import javax.annotation.Nullable;
import java.net.URI;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_ABSENT;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singletonMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.HEAD;
import static org.springframework.http.HttpMethod.OPTIONS;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.http.HttpStatus.Series.CLIENT_ERROR;
import static org.zalando.riptide.Bindings.on;
import static org.zalando.riptide.Navigators.series;
import static org.zalando.riptide.RoutingTree.dispatch;
import static org.zalando.riptide.compatibility.MockWebServerUtil.emptyMockResponse;
import static org.zalando.riptide.compatibility.MockWebServerUtil.getBaseUrl;
import static org.zalando.riptide.compatibility.MockWebServerUtil.getRecorderRequest;
import static org.zalando.riptide.compatibility.MockWebServerUtil.jsonMockResponse;
import static org.zalando.riptide.compatibility.MockWebServerUtil.verify;

final class HttpOperationsTest {

    private final MockWebServer server = new MockWebServer();

    private final Http http = Http.builder()
            .executor(Executors.newSingleThreadExecutor())
            .requestFactory(new HttpComponentsClientHttpRequestFactory())
            .baseUrl(getBaseUrl(server))
            .converter(new MappingJackson2HttpMessageConverter(
                    new ObjectMapper().setSerializationInclusion(NON_ABSENT)))
            .build();

    @SneakyThrows
    @AfterEach
    void tearDown() {
        server.shutdown();
    }

    static Iterable<Function<RestOperations, User>> get() {
        return Arrays.asList(
                unit -> unit.getForObject("/users/{id}", User.class, 1),
                unit -> unit.getForObject("/users/{id}", User.class, singletonMap("id", 1)),
                unit -> unit.getForObject(URI.create("/users/1"), User.class),
                unit -> unit.getForEntity("/users/{id}", User.class, 1).getBody(),
                unit -> unit.getForEntity("/users/{id}", User.class, singletonMap("id", 1)).getBody(),
                unit -> unit.getForEntity(URI.create("/users/1"), User.class).getBody()
        );
    }

    @ParameterizedTest
    @MethodSource("get")
    void shouldGet(final Function<RestOperations, User> test) {
        server.enqueue(jsonMockResponse("{\"name\":\"D. Fault\", \"birthday\":\"1984-09-13\"}"));

        final User user = test.apply(new HttpOperations(http));

        assertEquals("D. Fault", user.getName());
        assertEquals("1984-09-13", user.getBirthday());

        verify(server, 1, "/users/1");
    }

    static Iterable<Function<RestOperations, HttpHeaders>> head() {
        return Arrays.asList(
                unit -> unit.headForHeaders("/users/{id}", 1),
                unit -> unit.headForHeaders("/users/{id}", singletonMap("id", 1)),
                unit -> unit.headForHeaders(URI.create("/users/1"))
        );
    }

    @ParameterizedTest
    @MethodSource("head")
    void shouldHead(final Function<RestOperations, HttpHeaders> test) {
        server.enqueue(emptyMockResponse().setHeader("Test", "true"));
        final HttpHeaders headers = test.apply(new HttpOperations(http));

        assertEquals("true", headers.getFirst("Test"));

        var recorderRequest = getRecorderRequest(server);
        verifyRequest(recorderRequest, "/users/1", HEAD.toString());
    }

    static Iterable<Function<RestOperations, URI>> postForLocation() {
        final User user = new User("D. Fault", "1984-09-13");

        return Arrays.asList(
                unit -> unit.postForLocation("/departments/{id}/users", user, 1),
                unit -> unit.postForLocation("/departments/{id}/users", user, singletonMap("id", 1)),
                unit -> unit.postForLocation(URI.create("/departments/1/users"), user)
        );
    }

    @ParameterizedTest
    @MethodSource("postForLocation")
    void shouldPostForLocation(final Function<RestOperations, URI> test) {
        server.enqueue(emptyMockResponse().setHeader("Location", "/departments/1/users/1"));

        final URI location = test.apply(new HttpOperations(http));

        assertNotNull(location);
        assertEquals("/departments/1/users/1", location.toString());

        var recorderRequest = getRecorderRequest(server);
        verifyRequest(recorderRequest, "/departments/1/users", POST.toString());
        verifyRequestBody(recorderRequest, "{\"name\":\"D. Fault\",\"birthday\":\"1984-09-13\"}");
    }

    static Iterable<Function<RestOperations, User>> postForObject() {
        final User user = new User("D. Fault", "1984-09-13");

        return Arrays.asList(
                unit -> unit.postForObject("/departments/{id}/users", user, User.class, 1),
                unit -> unit.postForObject("/departments/{id}/users", user, User.class, singletonMap("id", 1)),
                unit -> unit.postForObject(URI.create("/departments/1/users"), user, User.class)
        );
    }

    @ParameterizedTest
    @MethodSource("postForObject")
    void shouldPostForObject(final Function<RestOperations, User> test) {
        server.enqueue(jsonMockResponse("{\"name\":\"D. Fault\",\"birthday\":\"1984-09-13\"}")
                .setHeader("Location", "/departments/1/users/1"));

        final User user = test.apply(new HttpOperations(http));

        assertEquals(new User("D. Fault", "1984-09-13"), user);

        var recorderRequest = getRecorderRequest(server);
        verifyRequest(recorderRequest, "/departments/1/users", POST.toString());
        verifyRequestBody(recorderRequest, "{\"name\":\"D. Fault\",\"birthday\":\"1984-09-13\"}");
    }

    static Iterable<Consumer<RestOperations>> put() {
        final User user = new User("D. Fault", "1984-09-13");

        return Arrays.asList(
                unit -> unit.put("/users/{id}", user, 1),
                unit -> unit.put("/users/{id}", user, singletonMap("id", 1)),
                unit -> unit.put(URI.create("/users/1"), user)
        );
    }

    @ParameterizedTest
    @MethodSource("put")
    void shouldPut(final Consumer<RestOperations> test) {
        server.enqueue(emptyMockResponse());

        test.accept(new HttpOperations(http));

        var recorderRequest = getRecorderRequest(server);
        verifyRequest(recorderRequest, "/users/1", PUT.toString());
        verifyRequestBody(recorderRequest, "{\"name\":\"D. Fault\",\"birthday\":\"1984-09-13\"}");
    }

    // needs concrete type to see patchForObject
    static Iterable<Function<HttpOperations, User>> patchForObject() {
        final User user = new User(null, "1984-09-13");

        return Arrays.asList(
                unit -> unit.patchForObject("/users/{id}", user, User.class, 1),
                unit -> unit.patchForObject("/users/{id}", user, User.class, singletonMap("id", 1)),
                unit -> unit.patchForObject(URI.create("/users/1"), user, User.class)
        );
    }

    @ParameterizedTest
    @MethodSource("patchForObject")
    void shouldPatchForObject(final Function<RestOperations, User> test) {
        server.enqueue(jsonMockResponse("{\"name\":\"D. Fault\",\"birthday\":\"1984-09-13\"}"));

        final User user = test.apply(new HttpOperations(http));

        assertEquals(new User("D. Fault", "1984-09-13"), user);

        var recorderRequest = getRecorderRequest(server);
        verifyRequest(recorderRequest, "/users/1", "PATCH");
        verifyRequestBody(recorderRequest, "{\"birthday\":\"1984-09-13\"}");
    }

    static Iterable<Consumer<RestOperations>> delete() {
        return Arrays.asList(
                unit -> unit.delete("/users/{id}", 1),
                unit -> unit.delete("/users/{id}", singletonMap("id", 1)),
                unit -> unit.delete(URI.create("/users/1"))
        );
    }

    @ParameterizedTest
    @MethodSource("delete")
    void shouldDelete(final Consumer<RestOperations> test) {
        server.enqueue(emptyMockResponse());

        test.accept(new HttpOperations(http));

        var recorderRequest = getRecorderRequest(server);
        verifyRequest(recorderRequest, "/users/1", DELETE.toString());
    }

    static Iterable<Function<RestOperations, Set<HttpMethod>>> optionsForAllow() {
        return Arrays.asList(
                unit -> unit.optionsForAllow("/users/{id}", 1),
                unit -> unit.optionsForAllow("/users/{id}", singletonMap("id", 1)),
                unit -> unit.optionsForAllow(URI.create("/users/1"))
        );
    }

    @ParameterizedTest
    @MethodSource("optionsForAllow")
    void shouldOptionsForAllow(final Function<RestOperations, Set<HttpMethod>> test) {
        server.enqueue(emptyMockResponse().setHeader("Allow", "GET, HEAD"));

        final Set<HttpMethod> allowed = test.apply(new HttpOperations(http));

        assertThat(allowed, contains(GET, HEAD));

        var recorderRequest = getRecorderRequest(server);
        verifyRequest(recorderRequest, "/users/1", OPTIONS.toString());
    }

    static Iterable<Function<RestOperations, User>> execute() {
        final ObjectMapper mapper = new ObjectMapper();

        final RequestCallback callback = request -> {
            request.getHeaders().add("Test", "true");
            request.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            mapper.writeValue(request.getBody(), new User("D. Fault", "1984-09-13"));
        };

        final ResponseExtractor<User> extractor = response ->
                mapper.readValue(response.getBody(), User.class);

        return Arrays.asList(
                unit -> unit.execute("/departments/{id}/users", POST, callback, extractor, 1),
                unit -> unit.execute("/departments/{id}/users", POST, callback, extractor, singletonMap("id", 1)),
                unit -> unit.execute(URI.create("/departments/1/users"), POST, callback, extractor)
        );
    }

    @ParameterizedTest
    @MethodSource("execute")
    void shouldExecute(final Function<RestOperations, User> test) {
        server.enqueue(jsonMockResponse("{\"name\":\"D. Fault\",\"birthday\":\"1984-09-13\"}"));

        final User user = test.apply(new HttpOperations(http));

        assertEquals(new User("D. Fault", "1984-09-13"), user);

        var recorderRequest = getRecorderRequest(server);
        verifyRequest(recorderRequest, "/departments/1/users", POST.toString());
        verifyRequestBody(recorderRequest, "{\"name\":\"D. Fault\",\"birthday\":\"1984-09-13\"}");
        assertEquals("true", recorderRequest.getHeaders().get("Test"));
    }

    @Test
    void shouldExchange() {
        server.enqueue(jsonMockResponse("{\"name\":\"D. Fault\",\"birthday\":\"1984-09-13\"}"));

        final User user = new HttpOperations(http).exchange(new RequestEntity<>(new User("D. Fault", "1984-09-13"),
                POST, URI.create("/departments/1/users")), User.class).getBody();

        assertEquals(new User("D. Fault", "1984-09-13"), user);

        var recorderRequest = getRecorderRequest(server);
        verifyRequest(recorderRequest, "/departments/1/users", POST.toString());
        verifyRequestBody(recorderRequest, "{\"name\":\"D. Fault\",\"birthday\":\"1984-09-13\"}");
    }

    @Test
    void shouldExecuteWithoutCallbackOrExtractor() {
        server.enqueue(emptyMockResponse());

        final RestOperations unit = new HttpOperations(http);
        @Nullable final User user = unit.execute("/departments/{id}/users", POST, null, null, 1);

        assertNull(user);

        var recorderRequest = getRecorderRequest(server);
        verifyRequest(recorderRequest, "/departments/1/users", POST.toString());
    }

    @Test
    void shouldFailToExpandUriTemplate() {
        final RestOperations unit = new HttpOperations(http);

        assertThrows(IllegalArgumentException.class, () ->
                unit.getForObject("/users/{id}", User.class, singletonMap("user_id", 1)));
    }

    @Test
    void shouldOverrideDefaultRoutingTree() {
        server.enqueue(new MockResponse().setResponseCode(404)
                .setBody("\"error\"")
                .setHeader("Content-Type","application/json"));


        final RestOperations unit = new HttpOperations(http)
                .withDefaultRoutingTree(dispatch(series(),
                        on(CLIENT_ERROR).call(String.class, error -> {
                            throw new UnsupportedOperationException(error);
                        })));

        final CompletionException exception = assertThrows(CompletionException.class, () ->
                unit.getForObject("/departments/{id}/users", User.class, 1));

        assertThat(exception.getCause(), is(instanceOf(UnsupportedOperationException.class)));
        assertEquals("error", exception.getCause().getMessage());

        verify(server, 1, "/departments/1/users");
    }

    private static void verifyRequestBody(RecordedRequest recordedRequest, String expectedBody) {
        assertEquals(expectedBody, recordedRequest.getBody().readString(UTF_8));
        assertEquals("application/json", recordedRequest.getHeaders().get("Content-Type"));
    }

    private static void verifyRequest(RecordedRequest recordedRequest,
                                      String expectedPath,
                                      String expectedMethod) {
        assertNotNull(recordedRequest);
        assertEquals(expectedPath, recordedRequest.getPath());
        assertEquals(expectedMethod, recordedRequest.getMethod());
    }
}

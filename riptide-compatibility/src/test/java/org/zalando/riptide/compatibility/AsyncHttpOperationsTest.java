package org.zalando.riptide.compatibility;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.restdriver.clientdriver.ClientDriver;
import com.github.restdriver.clientdriver.ClientDriverFactory;
import com.github.restdriver.clientdriver.ClientDriverRequest.Method;
import com.github.restdriver.clientdriver.ClientDriverResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.client.AsyncRequestCallback;
import org.springframework.web.client.AsyncRestOperations;
import org.springframework.web.client.ResponseExtractor;
import org.zalando.riptide.Http;

import javax.annotation.Nullable;
import java.net.URI;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.function.Function;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_ABSENT;
import static com.github.restdriver.clientdriver.RestClientDriver.giveEmptyResponse;
import static com.github.restdriver.clientdriver.RestClientDriver.giveResponse;
import static com.github.restdriver.clientdriver.RestClientDriver.onRequestTo;
import static java.util.Collections.singletonMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.HEAD;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.Series.CLIENT_ERROR;
import static org.zalando.riptide.Bindings.on;
import static org.zalando.riptide.Navigators.series;
import static org.zalando.riptide.RoutingTree.dispatch;

@SuppressWarnings("deprecation") // AsyncRestOperations
final class AsyncHttpOperationsTest {

    private final ClientDriver driver = new ClientDriverFactory().createClientDriver();

    private final Http http = Http.builder()
            .executor(Executors.newSingleThreadExecutor())
            .requestFactory(new HttpComponentsClientHttpRequestFactory())
            .baseUrl(driver.getBaseUrl())
            .converter(new MappingJackson2HttpMessageConverter(
                    new ObjectMapper().setSerializationInclusion(NON_ABSENT)))
            .build();

    @AfterEach
    void tearDown() {
        try {
            driver.verify();
        } finally {
            driver.shutdown();
        }
    }

    static Iterable<Function<AsyncRestOperations, ListenableFuture<ResponseEntity<User>>>> get() {
        return Arrays.asList(
                unit -> unit.getForEntity("/users/{id}", User.class, 1),
                unit -> unit.getForEntity("/users/{id}", User.class, singletonMap("id", 1)),
                unit -> unit.getForEntity(URI.create("/users/1"), User.class)
        );
    }

    @ParameterizedTest
    @MethodSource("get")
    void shouldGet(final Function<AsyncRestOperations, ListenableFuture<ResponseEntity<User>>> test)
            throws ExecutionException, InterruptedException {

        final ClientDriverResponse response =
                giveResponse("{\"name\":\"D. Fault\", \"birthday\":\"1984-09-13\"}", "application/json");

        driver.addExpectation(onRequestTo("/users/1"), response);

        final User user = test.apply(new AsyncHttpOperations(http)).get().getBody();

        assertNotNull(user);
        assertEquals("D. Fault", user.getName());
        assertEquals("1984-09-13", user.getBirthday());
    }

    static Iterable<Function<AsyncRestOperations, ListenableFuture<HttpHeaders>>> head() {
        return Arrays.asList(
                unit -> unit.headForHeaders("/users/{id}", 1),
                unit -> unit.headForHeaders("/users/{id}", singletonMap("id", 1)),
                unit -> unit.headForHeaders(URI.create("/users/1"))
        );
    }

    @ParameterizedTest
    @MethodSource("head")
    void shouldHead(final Function<AsyncRestOperations, ListenableFuture<HttpHeaders>> test)
            throws ExecutionException, InterruptedException {

        driver.addExpectation(onRequestTo("/users/1").withMethod(Method.HEAD),
                giveEmptyResponse().withHeader("Test", "true"));

        final HttpHeaders headers = test.apply(new AsyncHttpOperations(http)).get();

        assertEquals("true", headers.getFirst("Test"));
    }

    static Iterable<Function<AsyncRestOperations, ListenableFuture<URI>>> postForLocation() {
        final User user = new User("D. Fault", "1984-09-13");

        return Arrays.asList(
                unit -> unit.postForLocation("/departments/{id}/users", new HttpEntity<>(user), 1),
                unit -> unit.postForLocation("/departments/{id}/users", new HttpEntity<>(user), singletonMap("id", 1)),
                unit -> unit.postForLocation(URI.create("/departments/1/users"), new HttpEntity<>(user))
        );
    }

    @ParameterizedTest
    @MethodSource("postForLocation")
    void shouldPostForLocation(final Function<AsyncRestOperations, ListenableFuture<URI>> test)
            throws ExecutionException, InterruptedException {

        driver.addExpectation(onRequestTo("/departments/1/users")
                        .withMethod(Method.POST)
                        .withBody("{\"name\":\"D. Fault\",\"birthday\":\"1984-09-13\"}", "application/json"),
                giveEmptyResponse().withHeader("Location", "/departments/1/users/1"));

        final URI location = test.apply(new AsyncHttpOperations(http)).get();

        assertNotNull(location);
        assertEquals("/departments/1/users/1", location.toString());
    }

    static Iterable<Function<AsyncRestOperations, ListenableFuture<ResponseEntity<User>>>> postForObject() {
        final User user = new User("D. Fault", "1984-09-13");

        return Arrays.asList(
                unit -> unit.postForEntity("/departments/{id}/users", new HttpEntity<>(user), User.class, 1),
                unit -> unit.postForEntity("/departments/{id}/users", new HttpEntity<>(user), User.class, singletonMap("id", 1)),
                unit -> unit.postForEntity(URI.create("/departments/1/users"), new HttpEntity<>(user), User.class)
        );
    }

    @ParameterizedTest
    @MethodSource("postForObject")
    void shouldPostForObject(final Function<AsyncRestOperations, ListenableFuture<ResponseEntity<User>>> test)
            throws ExecutionException, InterruptedException {

        driver.addExpectation(onRequestTo("/departments/1/users")
                        .withMethod(Method.POST)
                        .withBody("{\"name\":\"D. Fault\",\"birthday\":\"1984-09-13\"}", "application/json"),
                giveResponse("{\"name\":\"D. Fault\",\"birthday\":\"1984-09-13\"}", "application/json")
                        .withHeader("Location", "/departments/1/users/1"));

        final User user = test.apply(new AsyncHttpOperations(http)).get().getBody();

        assertEquals(new User("D. Fault", "1984-09-13"), user);
    }

    static Iterable<Function<AsyncRestOperations, ListenableFuture<?>>> put() {
        final User user = new User("D. Fault", "1984-09-13");

        return Arrays.asList(
                unit -> unit.put("/users/{id}", new HttpEntity<>(user), 1),
                unit -> unit.put("/users/{id}", new HttpEntity<>(user), singletonMap("id", 1)),
                unit -> unit.put(URI.create("/users/1"), new HttpEntity<>(user))
        );
    }

    @ParameterizedTest
    @MethodSource("put")
    void shouldPut(final Function<AsyncRestOperations, ListenableFuture<?>> test)
            throws ExecutionException, InterruptedException {

        driver.addExpectation(onRequestTo("/users/1")
                        .withMethod(Method.PUT)
                        .withBody("{\"name\":\"D. Fault\",\"birthday\":\"1984-09-13\"}", "application/json"),
                giveEmptyResponse());

        test.apply(new AsyncHttpOperations(http)).get();
    }

    static Iterable<Function<AsyncRestOperations, ListenableFuture<?>>> delete() {
        return Arrays.asList(
                unit -> unit.delete("/users/{id}", 1),
                unit -> unit.delete("/users/{id}", singletonMap("id", 1)),
                unit -> unit.delete(URI.create("/users/1"))
        );
    }

    @ParameterizedTest
    @MethodSource("delete")
    void shouldDelete(final Function<AsyncRestOperations, ListenableFuture<?>> test)
            throws ExecutionException, InterruptedException {

        driver.addExpectation(onRequestTo("/users/1").withMethod(Method.DELETE),
                giveEmptyResponse());

        test.apply(new AsyncHttpOperations(http)).get();
    }

    static Iterable<Function<AsyncRestOperations, ListenableFuture<Set<HttpMethod>>>> optionsForAllow() {
        return Arrays.asList(
                unit -> unit.optionsForAllow("/users/{id}", 1),
                unit -> unit.optionsForAllow("/users/{id}", singletonMap("id", 1)),
                unit -> unit.optionsForAllow(URI.create("/users/1"))
        );
    }

    @ParameterizedTest
    @MethodSource("optionsForAllow")
    void shouldOptionsForAllow(final Function<AsyncRestOperations, ListenableFuture<Set<HttpMethod>>> test)
            throws ExecutionException, InterruptedException {

        driver.addExpectation(onRequestTo("/users/1").withMethod(Method.OPTIONS),
                giveEmptyResponse().withHeader("Allow", "GET, HEAD"));

        final Set<HttpMethod> allowed = test.apply(new AsyncHttpOperations(http)).get();

        assertThat(allowed, contains(GET, HEAD));
    }

    static Iterable<Function<AsyncRestOperations, ListenableFuture<User>>> execute() {
        final ObjectMapper mapper = new ObjectMapper();

        final AsyncRequestCallback callback = request -> {
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
    void shouldExecute(final Function<AsyncRestOperations, ListenableFuture<User>> test)
            throws ExecutionException, InterruptedException {

        driver.addExpectation(onRequestTo("/departments/1/users")
                        .withMethod(Method.POST)
                        .withBody("{\"name\":\"D. Fault\",\"birthday\":\"1984-09-13\"}", "application/json")
                        .withHeader("Test", "true"),
                giveResponse("{\"name\":\"D. Fault\",\"birthday\":\"1984-09-13\"}", "application/json"));

        final User user = test.apply(new AsyncHttpOperations(http)).get();

        assertEquals(new User("D. Fault", "1984-09-13"), user);
    }

    @Test
    void shouldExecuteWithoutCallbackOrExtractor() throws ExecutionException, InterruptedException {
        driver.addExpectation(onRequestTo("/departments/1/users")
                        .withMethod(Method.POST),
                giveEmptyResponse());

        final AsyncRestOperations unit = new AsyncHttpOperations(http);
        @Nullable final User user = unit.execute("/departments/{id}/users", POST, null,
                (ResponseExtractor<User>) null, 1).get();

        assertNull(user);
    }

    @Test
    void shouldAllowSynchronousAccess() {
        final AsyncRestOperations unit = new AsyncHttpOperations(http);
        assertNotNull(unit.getRestOperations());
    }

    @Test
    void shouldOverrideDefaultRoutingTree() {
        driver.addExpectation(onRequestTo("/departments/1/users"),
                giveResponse("\"error\"", "application/json").withStatus(404));

        final AsyncRestOperations unit = new AsyncHttpOperations(http)
                .withDefaultRoutingTree(dispatch(series(),
                        on(CLIENT_ERROR).call(String.class, error -> {
                            throw new UnsupportedOperationException(error);
                        })));

        final ExecutionException exception = assertThrows(ExecutionException.class, () ->
                unit.getForEntity("/departments/{id}/users", User.class, 1).get());

        assertThat(exception.getCause(), is(instanceOf(UnsupportedOperationException.class)));
        assertEquals("error", exception.getCause().getMessage());
    }

}

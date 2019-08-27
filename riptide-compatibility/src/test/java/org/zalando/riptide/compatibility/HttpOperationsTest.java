package org.zalando.riptide.compatibility;

import com.fasterxml.jackson.databind.*;
import com.github.restdriver.clientdriver.*;
import com.github.restdriver.clientdriver.ClientDriverRequest.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import org.springframework.http.*;
import org.springframework.http.client.*;
import org.springframework.http.converter.json.*;
import org.springframework.web.client.*;
import org.zalando.riptide.*;

import javax.annotation.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.*;
import static com.github.restdriver.clientdriver.RestClientDriver.*;
import static java.util.Collections.*;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.HttpMethod.*;
import static org.springframework.http.HttpStatus.Series.*;
import static org.zalando.riptide.Bindings.*;
import static org.zalando.riptide.Navigators.*;
import static org.zalando.riptide.RoutingTree.*;

final class HttpOperationsTest {

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
        final ClientDriverResponse response =
                giveResponse("{\"name\":\"D. Fault\", \"birthday\":\"1984-09-13\"}", "application/json");

        driver.addExpectation(onRequestTo("/users/1"), response);

        final User user = test.apply(new HttpOperations(http));

        assertEquals("D. Fault", user.getName());
        assertEquals("1984-09-13", user.getBirthday());
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
        driver.addExpectation(onRequestTo("/users/1").withMethod(Method.HEAD),
                giveEmptyResponse().withHeader("Test", "true"));

        final HttpHeaders headers = test.apply(new HttpOperations(http));

        assertEquals("true", headers.getFirst("Test"));
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
        driver.addExpectation(onRequestTo("/departments/1/users")
                        .withMethod(Method.POST)
                        .withBody("{\"name\":\"D. Fault\",\"birthday\":\"1984-09-13\"}", "application/json"),
                giveEmptyResponse().withHeader("Location", "/departments/1/users/1"));

        final URI location = test.apply(new HttpOperations(http));

        assertNotNull(location);
        assertEquals("/departments/1/users/1", location.toString());
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
        driver.addExpectation(onRequestTo("/departments/1/users")
                        .withMethod(Method.POST)
                        .withBody("{\"name\":\"D. Fault\",\"birthday\":\"1984-09-13\"}", "application/json"),
                giveResponse("{\"name\":\"D. Fault\",\"birthday\":\"1984-09-13\"}", "application/json")
                        .withHeader("Location", "/departments/1/users/1"));

        final User user = test.apply(new HttpOperations(http));

        assertEquals(new User("D. Fault", "1984-09-13"), user);
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
        driver.addExpectation(onRequestTo("/users/1")
                        .withMethod(Method.PUT)
                        .withBody("{\"name\":\"D. Fault\",\"birthday\":\"1984-09-13\"}", "application/json"),
                giveEmptyResponse());

        test.accept(new HttpOperations(http));
    }

    static Iterable<Function<RestOperations, User>> patchForObject() {
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
        driver.addExpectation(onRequestTo("/users/1")
                        .withMethod(Method.custom("PATCH"))
                        .withBody("{\"birthday\":\"1984-09-13\"}", "application/json"),
                giveResponse("{\"name\":\"D. Fault\",\"birthday\":\"1984-09-13\"}", "application/json"));

        final User user = test.apply(new HttpOperations(http));

        assertEquals(new User("D. Fault", "1984-09-13"), user);
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
        driver.addExpectation(onRequestTo("/users/1").withMethod(Method.DELETE),
                giveEmptyResponse());

        test.accept(new HttpOperations(http));
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
        driver.addExpectation(onRequestTo("/users/1").withMethod(Method.OPTIONS),
                giveEmptyResponse().withHeader("Allow", "GET, HEAD"));

        final Set<HttpMethod> allowed = test.apply(new HttpOperations(http));

        assertThat(allowed, contains(GET, HEAD));
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
        driver.addExpectation(onRequestTo("/departments/1/users")
                        .withMethod(Method.POST)
                        .withBody("{\"name\":\"D. Fault\",\"birthday\":\"1984-09-13\"}", "application/json")
                        .withHeader("Test", "true"),
                giveResponse("{\"name\":\"D. Fault\",\"birthday\":\"1984-09-13\"}", "application/json"));

        final User user = test.apply(new HttpOperations(http));

        assertEquals(new User("D. Fault", "1984-09-13"), user);
    }

    @Test
    void shouldExchange() {
        driver.addExpectation(onRequestTo("/departments/1/users")
                        .withMethod(Method.POST)
                        .withBody("{\"name\":\"D. Fault\",\"birthday\":\"1984-09-13\"}", "application/json"),
                giveResponse("{\"name\":\"D. Fault\",\"birthday\":\"1984-09-13\"}", "application/json"));

        final User user = new HttpOperations(http).exchange(new RequestEntity<>(new User("D. Fault", "1984-09-13"),
                POST, URI.create("/departments/1/users")), User.class).getBody();

        assertEquals(new User("D. Fault", "1984-09-13"), user);
    }

    @Test
    void shouldExecuteWithoutCallbackOrExtractor() {
        driver.addExpectation(onRequestTo("/departments/1/users")
                        .withMethod(Method.POST),
                giveEmptyResponse());

        final RestOperations unit = new HttpOperations(http);
        @Nullable final User user = unit.execute("/departments/{id}/users", POST, null, null, 1);

        assertNull(user);
    }

    @Test
    void shouldFailToExpandUriTemplate() {
        final RestOperations unit = new HttpOperations(http);

        assertThrows(IllegalArgumentException.class, () ->
                unit.getForObject("/users/{id}", User.class, singletonMap("user_id", 1)));
    }

    @Test
    void shouldOverrideDefaultRoutingTree() {
        driver.addExpectation(onRequestTo("/departments/1/users"),
                giveResponse("\"error\"", "application/json").withStatus(404));

        final RestOperations unit = new HttpOperations(http)
                .withDefaultRoutingTree(dispatch(series(),
                        on(CLIENT_ERROR).call(String.class, error -> {
                            throw new UnsupportedOperationException(error);
                        })));

        final CompletionException exception = assertThrows(CompletionException.class, () ->
                unit.getForObject("/departments/{id}/users", User.class, 1));

        assertThat(exception.getCause(), is(instanceOf(UnsupportedOperationException.class)));
        assertEquals("error", exception.getCause().getMessage());
    }

}

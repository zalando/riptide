package org.zalando.riptide.stream;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.restdriver.clientdriver.ClientDriverRule;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.http.client.ClientHttpResponse;
import org.zalando.riptide.Http;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NON_PRIVATE;
import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static com.github.restdriver.clientdriver.RestClientDriver.giveEmptyResponse;
import static com.github.restdriver.clientdriver.RestClientDriver.giveResponseAsBytes;
import static com.github.restdriver.clientdriver.RestClientDriver.onRequestTo;
import static com.google.common.io.Resources.getResource;
import static java.util.Collections.singletonList;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.springframework.http.HttpStatus.Series.SUCCESSFUL;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.zalando.riptide.Bindings.on;
import static org.zalando.riptide.HttpBuilder.simpleRequestFactory;
import static org.zalando.riptide.Navigators.reasonPhrase;
import static org.zalando.riptide.Navigators.series;
import static org.zalando.riptide.PassRoute.pass;
import static org.zalando.riptide.stream.Streams.streamConverter;
import static org.zalando.riptide.stream.Streams.streamOf;

public final class StreamIOTest {

    @Rule
    public final ClientDriverRule driver = new ClientDriverRule();

    public final ExpectedException exception = ExpectedException.none();

    @JsonAutoDetect(fieldVisibility = NON_PRIVATE)
    static class User {
        String login;

        public String getLogin() {
            return login;
        }
    }

    private final ExecutorService executor = newSingleThreadExecutor();

    private final Http http = Http.builder()
            .baseUrl(driver.getBaseUrl())
            .configure(simpleRequestFactory(newSingleThreadExecutor()))
            .converter(streamConverter(new ObjectMapper().disable(FAIL_ON_UNKNOWN_PROPERTIES), singletonList(APPLICATION_JSON)))
            .build();

    @After
    public void shutdownExecutor() {
        executor.shutdown();
    }


    @Test
    public void shouldReadContributors() throws IOException {
        driver.addExpectation(onRequestTo("/repos/zalando/riptide/contributors"),
                giveResponseAsBytes(getResource("contributors.json").openStream(), "application/json"));

        final AtomicReference<Stream<User>> reference = new AtomicReference<>();

        http.get("/repos/{org}/{repo}/contributors", "zalando", "riptide")
                .dispatch(series(),
                        on(SUCCESSFUL).call(streamOf(User.class), reference::set)).join();

        final List<String> users = reference.get()
                .map(User::getLogin)
                .collect(toList());

        assertThat(users, hasItems("jhorstmann", "lukasniemeier-zalando", "whiskeysierra"));
    }

    @Test
    public void shouldCancelRequest() throws IOException {
        driver.addExpectation(onRequestTo("/repos/zalando/riptide/contributors"),
                giveResponseAsBytes(getResource("contributors.json").openStream(), "application/json"));

        final CompletableFuture<Void> future = http.get("/repos/{org}/{repo}/contributors", "zalando", "riptide")
                .dispatch(series(),
                        on(SUCCESSFUL).call(pass()));

        future.cancel(true);

        try {
            future.join();
        } catch (final CancellationException e) {
            // expected
        }

        // we don't care whether the request was actually made or not, but by default the driver will verify
        // all expectations after every tests
        driver.reset();
    }

    @Test
    public void shouldFailOnResponse() throws IOException {
        driver.addExpectation(onRequestTo("/repos/zalando/riptide/contributors"),
                giveResponseAsBytes(getResource("contributors.json").openStream(), "application/json")
                        .after(1, TimeUnit.SECONDS));

        exception.expect(CompletionException.class);
        exception.expectCause(instanceOf(IOException.class));

        http.get("/repos/{org}/{repo}/contributors", "zalando", "riptide")
                .dispatch(reasonPhrase(),
                        on("OK").call(ClientHttpResponse::close)).join();
    }

    @Test
    public void shouldReadEmptyResponse() {
        driver.addExpectation(onRequestTo("/repos/zalando/riptide/contributors"),
                giveEmptyResponse().withStatus(200));

        http.get("/repos/{org}/{repo}/contributors", "zalando", "riptide")
                .dispatch(reasonPhrase(),
                        on("OK").call(ClientHttpResponse::close)).join();
    }

}

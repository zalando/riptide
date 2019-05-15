package org.zalando.riptide;

import com.github.restdriver.clientdriver.ClientDriver;
import com.github.restdriver.clientdriver.ClientDriverFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static com.github.restdriver.clientdriver.RestClientDriver.giveResponse;
import static com.github.restdriver.clientdriver.RestClientDriver.onRequestTo;
import static com.google.common.base.Throwables.getStackTraceAsString;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.zalando.riptide.Navigators.contentType;

final class OriginalStackTracePluginTest {

    private final ClientDriver driver = new ClientDriverFactory().createClientDriver();
    private final ExecutorService executor = newSingleThreadExecutor();

    @BeforeEach
    void setUp() {
        driver.addExpectation(onRequestTo("/"),
                giveResponse("", "application/json"));
    }

    @AfterEach
    void tearDown() throws Exception {
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
    }

    @Test
    void shouldKeepOriginalStackTrace() {
        final Http unit = configureRest().build();
        final CompletableFuture<ClientHttpResponse> future = execute(unit.get("/"));
        final Exception exception = perform(future);

        assertThat(exception, is(instanceOf(CompletionException.class)));
        assertThat(exception.getCause(), is(instanceOf(UnexpectedResponseException.class)));

        assertThat(getStackTraceAsString(exception), containsString("Requester$ResponseDispatcher.call("));
        assertThat(getStackTraceAsString(exception), containsString("OriginalStackTracePluginTest.execute("));
    }

    @Test
    void shouldNotKeepOriginalStackTrace() {
        final Http unit = configureRest().plugin(new Plugin() {
        }).build();
        final CompletableFuture<ClientHttpResponse> future = execute(unit.get("/"));
        final Exception exception = perform(future);

        assertThat(exception, is(instanceOf(CompletionException.class)));
        assertThat(exception.getCause(), is(instanceOf(UnexpectedResponseException.class)));

        assertThat(getStackTraceAsString(exception), not(containsString("Requester$ResponseDispatcher.call(")));
        assertThat(getStackTraceAsString(exception), not(containsString("OriginalStackTracePluginTest.execute(")));
    }

    private Http.ConfigurationStage configureRest() {
        return Http.builder()
                .executor(executor)
                .requestFactory(new SimpleClientHttpRequestFactory())
                .baseUrl(driver.getBaseUrl());
    }

    private CompletableFuture<ClientHttpResponse> execute(final DispatchStage stage) {
        return stage.dispatch(contentType());
    }

    private Exception perform(final CompletableFuture<ClientHttpResponse> future) {
        try {
            future.join();
            throw new AssertionError("Expected exception");
        } catch (final Exception e) {
            return e;
        }
    }

}

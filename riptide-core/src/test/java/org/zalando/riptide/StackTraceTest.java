package org.zalando.riptide;

import com.github.restdriver.clientdriver.ClientDriverRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static com.github.restdriver.clientdriver.RestClientDriver.giveResponse;
import static com.github.restdriver.clientdriver.RestClientDriver.onRequestTo;
import static com.google.common.base.Throwables.getStackTraceAsString;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.zalando.riptide.HttpBuilder.simpleRequestFactory;
import static org.zalando.riptide.Navigators.contentType;

public final class StackTraceTest {

    @Rule
    public final ClientDriverRule driver = new ClientDriverRule();

    private final ExecutorService executor = newSingleThreadExecutor();

    @Before
    public void setUp() throws Exception {
        driver.addExpectation(onRequestTo("/"),
                giveResponse("", "application/json"));
    }

    @After
    public void tearDown() throws Exception {
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
    }

    @Test
    public void shouldKeepOriginalStackTrace() throws Exception {
        final Http unit = configureRest().build();
        final CompletableFuture<Void> future = execute(unit.get("/"));
        final Exception exception = perform(future);

        assertThat(exception, is(instanceOf(CompletionException.class)));
        assertThat(exception.getCause(), is(instanceOf(NoRouteException.class)));

        assertThat(getStackTraceAsString(exception), containsString("Requester$ResponseDispatcher.call("));
        assertThat(getStackTraceAsString(exception), containsString("StackTraceTest.execute("));
    }

    @Test
    public void shouldNotKeepOriginalStackTrace() throws Exception {
        final Http unit = configureRest().plugin((arguments, execution) -> execution).build();
        final CompletableFuture<Void> future = execute(unit.get("/"));
        final Exception exception = perform(future);

        assertThat(exception, is(instanceOf(CompletionException.class)));
        assertThat(exception.getCause(), is(instanceOf(NoRouteException.class)));

        assertThat(getStackTraceAsString(exception), not(containsString("Requester$ResponseDispatcher.call(")));
        assertThat(getStackTraceAsString(exception), not(containsString("StackTraceTest.execute(")));
    }

    private HttpBuilder configureRest() {
        return Http.builder()
                .baseUrl(driver.getBaseUrl())
                .configure(simpleRequestFactory(executor));
    }

    private CompletableFuture<Void> execute(final Requester requester) {
        return requester.dispatch(contentType());
    }

    private Exception perform(final CompletableFuture<Void> future) {
        try {
            future.join();
            throw new AssertionError("Expected exception");
        } catch (final Exception e) {
            return e;
        }
    }

}

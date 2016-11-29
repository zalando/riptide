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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.zalando.riptide.Navigators.contentType;
import static org.zalando.riptide.RestBuilder.simpleRequestFactory;

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
        final Rest unit = configureRest().build();
        final CompletableFuture<Void> future = execute(unit.get("/"));
        final Exception exception = perform(future);

        assertThat(exception, is(instanceOf(CompletionException.class)));
        assertThat(exception.getCause(), is(instanceOf(NoRouteException.class)));

        assertThat(getStackTraceAsString(exception), containsString("Requester$ResponseDispatcher.dispatch("));
        assertThat(getStackTraceAsString(exception), containsString("StackTraceTest.execute("));
    }

    private RestBuilder configureRest() {
        return Rest.builder()
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

package org.zalando.riptide;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.restdriver.clientdriver.ClientDriverRule;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NON_PRIVATE;
import static com.github.restdriver.clientdriver.RestClientDriver.giveEmptyResponse;
import static com.github.restdriver.clientdriver.RestClientDriver.giveResponseAsBytes;
import static com.github.restdriver.clientdriver.RestClientDriver.onRequestTo;
import static com.google.common.io.Resources.getResource;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.Assert.assertThat;
import static org.springframework.http.HttpStatus.Series.SUCCESSFUL;
import static org.zalando.riptide.Bindings.on;
import static org.zalando.riptide.Navigators.series;
import static org.zalando.riptide.HttpBuilder.simpleRequestFactory;
import static org.zalando.riptide.Route.listOf;
import static org.zalando.riptide.PassRoute.pass;

public final class IOTest {

    @Rule
    public final ClientDriverRule driver = new ClientDriverRule();

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
            .configure(simpleRequestFactory(executor))
            .converter(createJsonConverter())
            .build();

    private MappingJackson2HttpMessageConverter createJsonConverter() {
        final MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setObjectMapper(new ObjectMapper().findAndRegisterModules()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES));
        return converter;
    }

    @After
    public void tearDown() {
        executor.shutdown();
    }

    @Test
    public void shouldReadContributors() throws IOException {
        driver.addExpectation(onRequestTo("/repos/zalando/riptide/contributors"),
                giveResponseAsBytes(getResource("contributors.json").openStream(), "application/json"));

        final AtomicReference<List<User>> reference = new AtomicReference<>();

        http.get("/repos/{org}/{repo}/contributors", "zalando", "riptide")
                .dispatch(series(),
                        on(SUCCESSFUL).call(listOf(User.class), reference::set)).join();

        final List<String> users = reference.get().stream()
                .map(User::getLogin)
                .collect(toList());

        assertThat(users, hasItems("jhorstmann", "lukasniemeier-zalando", "whiskeysierra"));
    }

    @Test
    public void shouldCancelRequest() throws ExecutionException, InterruptedException {
        driver.addExpectation(onRequestTo("/repos/zalando/riptide/contributors"),
                giveEmptyResponse().after(1, TimeUnit.SECONDS));

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

}

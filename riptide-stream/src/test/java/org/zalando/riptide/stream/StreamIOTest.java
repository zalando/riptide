package org.zalando.riptide.stream;

/*
 * ⁣​
 * Riptide
 * ⁣⁣
 * Copyright (C) 2015 - 2016 Zalando SE
 * ⁣⁣
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ​⁣
 */

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.github.restdriver.clientdriver.ClientDriverRule;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.http.client.ClientHttpResponse;
import org.zalando.riptide.Completion;
import org.zalando.riptide.Rest;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NON_PRIVATE;
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
import static org.zalando.riptide.Navigators.reasonPhrase;
import static org.zalando.riptide.Navigators.series;
import static org.zalando.riptide.RestBuilder.simpleRequestFactory;
import static org.zalando.riptide.Route.pass;
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

    private final Rest rest = Rest.builder()
            .baseUrl(driver.getBaseUrl())
            .configure(simpleRequestFactory(newSingleThreadExecutor()))
            .converter(streamConverter(null, singletonList(APPLICATION_JSON)))
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

        rest.get("/repos/{org}/{repo}/contributors", "zalando", "riptide")
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

        final Completion<Void> future = rest.get("/repos/{org}/{repo}/contributors", "zalando", "riptide")
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

        rest.get("/repos/{org}/{repo}/contributors", "zalando", "riptide")
                .dispatch(reasonPhrase(),
                        on("OK").call(ClientHttpResponse::close)).join();
    }

    @Test
    public void shouldReadEmptyResponse() {
        driver.addExpectation(onRequestTo("/repos/zalando/riptide/contributors"),
                giveEmptyResponse().withStatus(200));

        rest.get("/repos/{org}/{repo}/contributors", "zalando", "riptide")
                .dispatch(reasonPhrase(),
                        on("OK").call(ClientHttpResponse::close)).join();
    }

}

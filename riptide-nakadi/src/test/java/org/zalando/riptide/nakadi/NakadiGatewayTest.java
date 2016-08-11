package org.zalando.riptide.nakadi;

/*
 * ⁣​
 * Riptide: Nakadi
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

import static com.github.restdriver.clientdriver.ClientDriverRequest.Method.GET;
import static com.github.restdriver.clientdriver.ClientDriverRequest.Method.POST;
import static com.github.restdriver.clientdriver.ClientDriverRequest.Method.PUT;
import static com.github.restdriver.clientdriver.RestClientDriver.giveResponse;
import static com.github.restdriver.clientdriver.RestClientDriver.giveResponseAsBytes;
import static com.github.restdriver.clientdriver.RestClientDriver.onRequestTo;
import static com.google.common.io.Resources.getResource;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.zalando.riptide.nakadi.MockSetup.defaultClient;
import static org.zalando.riptide.nakadi.MockSetup.defaultConfig;
import static org.zalando.riptide.nakadi.MockSetup.defaultConverters;
import static org.zalando.riptide.nakadi.MockSetup.defaultFactory;
import static org.zalando.riptide.nakadi.NakadiGateway.PROBLEM;
import static org.zalando.riptide.stream.Streams.APPLICATION_X_JSON_STREAM;

import java.io.IOException;
import java.io.OutputStream;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.http.client.config.RequestConfig;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zalando.problem.ThrowableProblem;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.restdriver.clientdriver.ClientDriverRule;

public class NakadiGatewayTest {

    private static final Logger LOG = LoggerFactory.getLogger(NakadiGatewayTest.class);

    private static final String[] DEFAULT_CURSORS_LIST = {
            "[{\"partition\":\"0\",\"offset\":\"0\"}]",
            "[{\"partition\":\"0\",\"offset\":\"1\"}]",
            "[{\"partition\":\"0\",\"offset\":\"2\"}]",
            "[{\"partition\":\"0\",\"offset\":\"3\"}]",
            "[{\"partition\":\"0\",\"offset\":\"4\"}]",
            "[{\"partition\":\"0\",\"offset\":\"5\"}]",
            "[{\"partition\":\"0\",\"offset\":\"6\"}]"
    };

    @Rule
    public final ClientDriverRule driver = new ClientDriverRule();

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    private final NakadiGateway unit;

    public NakadiGatewayTest() {
        unit = new MockSetup(driver).getNakadi();
    }

    @Before
    public void reset() throws IOException {
        driver.reset();

        driver.addExpectation(onRequestTo("/subscriptions").withMethod(POST),
                giveResponseAsBytes(getResource("subscription.json").openStream(),
                        APPLICATION_JSON.toString()));
        driver.addExpectation(onRequestTo("/subscriptions/my-stream/events").withMethod(GET),
                giveResponseAsBytes(getResource("event-stream.json").openStream(),
                        APPLICATION_X_JSON_STREAM.toString()));
    }

    @Test
    public void shouldReadBatchesFromStream() throws Throwable {

        for (String cursors : DEFAULT_CURSORS_LIST) {
            driver.addExpectation(onRequestTo("/subscriptions/my-stream/cursors")
                    .withMethod(PUT).withBody(cursors, APPLICATION_JSON.toString()),
                    giveResponse(cursors, APPLICATION_JSON.toString()));
        }

        AtomicInteger counter = new AtomicInteger();
        
        unit.stream("me", singletonList("event-type"), event -> counter.incrementAndGet());

        assertThat(counter.get(), is(equalTo(6)));
    }

    @Test
    public void shouldExposeConsumerException() throws Throwable {
        exception.expect(RuntimeException.class);

        String cursors = DEFAULT_CURSORS_LIST[0];
        driver.addExpectation(onRequestTo("/subscriptions/my-stream/cursors")
                .withMethod(PUT).withBody(cursors, APPLICATION_JSON.toString()),
                giveResponse(cursors, APPLICATION_JSON.toString()));

        try {
            unit.stream("me", singletonList("event-type"), event -> {
                throw new RuntimeException();
            });
        } catch (ExecutionException ex) {
            throw ex.getCause();
        }
    }

    @Test
    public void shouldExposeTimeoutException() throws Throwable {
        exception.expect(NakadiGateway.GatewayException.class);
        exception.expectCause(instanceOf(SocketTimeoutException.class));

        String cursors = DEFAULT_CURSORS_LIST[0];
        driver.addExpectation(onRequestTo("/subscriptions/my-stream/cursors")
                .withMethod(PUT).withBody(cursors, APPLICATION_JSON.toString()),
                giveResponse(cursors, APPLICATION_JSON.toString()).after(1, TimeUnit.SECONDS));

        final RequestConfig config = RequestConfig.copy(defaultConfig()).setSocketTimeout(500).build();
        final MockSetup setup = new MockSetup(driver, defaultConverters(), defaultFactory(defaultClient(config, 2)));
        final NakadiGateway unit = setup.getNakadi();

        try {
            unit.stream("me", singletonList("event-type"), event -> LOG.info("consumed event: {}", event));
        } catch (ExecutionException ex) {
            throw ex.getCause();
        }
    }

    @Test
    public void shouldExposeInterruptException() throws Throwable {
        exception.expect(NakadiGateway.GatewayException.class);
        exception.expectCause(instanceOf(InterruptedException.class));

        for (String cursors : DEFAULT_CURSORS_LIST) {
            driver.addExpectation(onRequestTo("/subscriptions/my-stream/cursors")
                    .withMethod(PUT).withBody(cursors, APPLICATION_JSON.toString()),
                    giveResponse(cursors, APPLICATION_JSON.toString())
                            .after(500, TimeUnit.MILLISECONDS));
        }

        final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        try {
            unit.stream("me", singletonList("event-type"), event -> {
                final Thread thread = Thread.currentThread();
                executor.schedule(thread::interrupt, 10, TimeUnit.MILLISECONDS);
                LOG.info("consumed event: {}", event);
            });
        } catch (ExecutionException ex) {
            throw ex.getCause();
        } finally {
            executor.shutdown();
        }
    }

    @Test
    public void shouldExposeIOException() throws Throwable {
        exception.expect(NakadiGateway.GatewayException.class);
        exception.expectCause(instanceOf(IOException.class));

        String cursors = DEFAULT_CURSORS_LIST[0];
        driver.addExpectation(onRequestTo("/subscriptions/my-stream/cursors")
                .withMethod(PUT).withBody(cursors, APPLICATION_JSON.toString()),
                giveResponse(cursors, APPLICATION_JSON.toString()));

        final JsonFactory factory = Mockito.spy(new JsonFactory());
        final ObjectMapper mapper = new ObjectMapper(factory).findAndRegisterModules();
        final MockSetup setup = new MockSetup(driver, defaultConverters(mapper), defaultFactory());
        final NakadiGateway unit = setup.getNakadi();

        Mockito.doCallRealMethod()
                .doCallRealMethod()
                .doThrow(new IOException())
                .when(factory)
                .createGenerator(any(OutputStream.class),
                        eq(JsonEncoding.UTF8));

        try {
            unit.stream("me", singletonList("event-type"), event -> LOG.info("consumed event: {}", event));
        } catch (ExecutionException ex) {
            throw ex.getCause();
        }
    }

    @Test
    public void shouldExposeFailedCommit() throws Throwable {
        exception.expect(NakadiGateway.GatewayException.class);
        exception.expectCause(instanceOf(ThrowableProblem.class));

        driver.addExpectation(onRequestTo("/subscriptions/my-stream/cursors").withMethod(PUT),
                giveResponseAsBytes(getResource("problem.json").openStream(), PROBLEM.toString())
                        .withStatus(500));

        try {
            unit.stream("me", singletonList("event-type"), event -> LOG.info("consumed event: {}", event));
        } catch (ExecutionException ex) {
            throw ex.getCause();
        }
    }
}

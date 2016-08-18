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
import static org.hobsoft.hamcrest.compose.ComposeMatchers.hasFeature;
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

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.SocketTimeoutException;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.http.client.config.RequestConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zalando.problem.ThrowableProblem;
import org.zalando.riptide.NoRouteException;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.restdriver.clientdriver.ClientDriverRule;
import com.google.common.net.MediaType;

@net.jcip.annotations.NotThreadSafe
public class NakadiGatewayTest {

    private static final Logger LOG = LoggerFactory.getLogger(NakadiGatewayTest.class);

    private static final String[] DEFAULT_CURSORS_LIST = {
            "[{\"partition\":\"0\",\"offset\":\"2\"}]",
            "[{\"partition\":\"0\",\"offset\":\"3\"}]",
            "[{\"partition\":\"0\",\"offset\":\"4\"}]",
            "[{\"partition\":\"0\",\"offset\":\"5\"}]",
            "[{\"partition\":\"0\",\"offset\":\"6\"}]"
    };

    private static final String SUBSCRIPTION_RESPONSE = "{\"id\":\"my-stream\"," +
            "\"owning_application\":\"owner\",\"consumer_group\":\"group\"," +
            "\"event_types\":[\"event-type\"],\"read_from\":\"begin\"," +
            "\"created_at\":\"2016-12-24T18:59:30.123456\"}";

    private static final String PROBLEM_RESPONSE = "{\"type\":\"unknown\",\"title\":\"title\"," +
            "\"status\":500,\"detail\":\"internal\",\"instance\":\"commit\"}";

    private static final String INVALID_RESPONSE = "{\"unknown\":\"unknown\"}";

    @Rule
    public final ClientDriverRule driver = new ClientDriverRule();

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Rule
    public final TestLogger logger = new TestLogger();

    private final Subscription subscription = new Subscription("owner", "group", singletonList("event-type"), "begin");
    private final NakadiGateway unit;

    public NakadiGatewayTest() {
        unit = new MockSetup(driver).getNakadi();
    }

    @Before
    public void before() throws IOException {
        driver.addExpectation(onRequestTo("/subscriptions").withMethod(POST),
                giveResponse(SUBSCRIPTION_RESPONSE, APPLICATION_JSON.toString()));
        driver.addExpectation(onRequestTo("/subscriptions/my-stream/events").withMethod(GET),
                giveResponseAsBytes(getResource("event-stream.json").openStream(),
                        APPLICATION_X_JSON_STREAM.toString())
                                .withHeader(NakadiGateway.SESSION_HEADER, UUID.randomUUID().toString()));
    }

    @After
    public void after() {
        driver.reset();
    }

    @Test
    public void shouldReadBatchesFromStream() throws Throwable {

        for (String cursors : DEFAULT_CURSORS_LIST) {
            driver.addExpectation(onRequestTo("/subscriptions/my-stream/cursors")
                    .withMethod(PUT).withBody(cursors, APPLICATION_JSON.toString()),
                    giveResponse(cursors, APPLICATION_JSON.toString()));
        }

        AtomicInteger counter = new AtomicInteger();
        unit.stream(unit.subscribe(subscription), event -> counter.incrementAndGet());

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
            unit.stream(unit.subscribe(subscription), event -> {
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

        final String cursors = DEFAULT_CURSORS_LIST[0];
        driver.addExpectation(onRequestTo("/subscriptions/my-stream/cursors")
                .withMethod(PUT).withBody(cursors, APPLICATION_JSON.toString()),
                giveResponse(cursors, APPLICATION_JSON.toString()).after(1, TimeUnit.SECONDS));

        final RequestConfig config = RequestConfig.copy(defaultConfig()).setSocketTimeout(500).build();
        final MockSetup setup = new MockSetup(driver, defaultConverters(), defaultFactory(defaultClient(config, 2)));
        final NakadiGateway unit = setup.getNakadi();

        try {
            unit.stream(unit.subscribe(subscription), event -> LOG.info("consumed event: {}", event));
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
            unit.stream(unit.subscribe(subscription), event -> {
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

        final String cursors = DEFAULT_CURSORS_LIST[0];
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
            unit.stream(unit.subscribe(subscription), event -> LOG.info("consumed event: {}", event));
        } catch (ExecutionException ex) {
            throw ex.getCause();
        }
    }

    @Test
    public void shouldExposeFailureInCommit() throws Throwable {
        exception.expect(NakadiGateway.GatewayException.class);
        exception.expectCause(instanceOf(IOException.class));
        exception.expectCause(hasFeature(Throwable::getCause,instanceOf(ThrowableProblem.class)));

        driver.addExpectation(onRequestTo("/subscriptions/my-stream/cursors").withMethod(PUT),
                giveResponse(PROBLEM_RESPONSE, PROBLEM.toString()).withStatus(500));

        try {
            unit.stream(unit.subscribe(subscription), event -> LOG.info("consumed event: {}", event));
        } catch (ExecutionException ex) {
            throw ex.getCause();
        }
    }

    @Test
    public void shouldExposeFailureOnUnexpectedResponse() throws Throwable {
        exception.expect(NakadiGateway.GatewayException.class);
        exception.expectCause(instanceOf(NoRouteException.class));

        driver.addExpectation(onRequestTo("/subscriptions/my-stream/cursors").withMethod(PUT),
                giveResponse("failure text message", MediaType.ANY_TEXT_TYPE.toString()).withStatus(400));

        try {
            unit.stream(unit.subscribe(subscription), event -> LOG.info("consumed event: {}", event));
        } catch (ExecutionException ex) {
            throw ex.getCause();
        }
    }

    @Test
    public void shouldExposeInvalidResponse() throws Throwable {
        exception.expect(NakadiGateway.GatewayException.class);
        exception.expectCause(instanceOf(NoRouteException.class));

        driver.addExpectation(onRequestTo("/subscriptions/my-stream/cursors").withMethod(PUT),
                giveResponse("failure text message", MediaType.ANY_TEXT_TYPE.toString()).withStatus(400));

        try {
            unit.stream(unit.subscribe(subscription), event -> LOG.info("consumed event: {}", event));
        } catch (ExecutionException ex) {
            throw ex.getCause();
        }
    }

    @Test
    @Ignore("does only work with com.github.rest-driver/rest-client-driver:2.0.1-SNAPSHOT"
            + "build from https://github.com/tkrop/rest-driver/tree/feature/stream-and-lamda-support")
    public void shouldAbortConnectionOnException() throws Throwable {
        exception.expect(RuntimeException.class);

        driver.reset();
        driver.addExpectation(onRequestTo("/subscriptions").withMethod(POST),
                giveResponse(SUBSCRIPTION_RESPONSE, APPLICATION_JSON.toString()));
        try (final FilterInputStream content =
                new FilterInputStream(getResource("event-stream.json").openStream()) {
                    public boolean closed = false;

                    @Override
                    public int read(byte[] b, int off, int len) throws IOException {
                        int read = super.read(b, off, len);
                        return read < 0 && !closed ? 0 : read;
                    }

                    @Override
                    public void close() throws IOException {
                        if (!this.closed) {
                            this.closed = true;
                            super.close();
                        }
                    }
                }) {
            driver.addExpectation(onRequestTo("/subscriptions/my-stream/events").withMethod(GET),
                    giveResponseAsBytes(content, APPLICATION_X_JSON_STREAM.toString())
                            .withHeader(NakadiGateway.SESSION_HEADER, UUID.randomUUID().toString()));

            for (String cursors : DEFAULT_CURSORS_LIST) {
                driver.addExpectation(onRequestTo("/subscriptions/my-stream/cursors")
                        .withMethod(PUT).withBody(cursors, APPLICATION_JSON.toString()),
                        giveResponse(cursors, APPLICATION_JSON.toString()));
            }

            final AtomicInteger counter = new AtomicInteger();

            try {
                unit.stream(unit.subscribe(subscription), event -> {
                    if (counter.incrementAndGet() == 5) {
                        throw new RuntimeException();
                    }
                });
            } catch (ExecutionException ex) {
                throw ex.getCause();
            }
        }
    }

}

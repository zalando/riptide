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
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hobsoft.hamcrest.compose.ComposeMatchers.hasFeature;
import static org.junit.Assert.assertThat;
import static org.junit.internal.matchers.ThrowableCauseMatcher.hasCause;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doThrow;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.zalando.riptide.nakadi.MockSetup.defaultClient;
import static org.zalando.riptide.nakadi.MockSetup.defaultConfig;
import static org.zalando.riptide.nakadi.MockSetup.defaultConverters;
import static org.zalando.riptide.nakadi.MockSetup.defaultFactory;
import static org.zalando.riptide.nakadi.NakadiGateway.PROBLEM;
import static org.zalando.riptide.stream.Streams.APPLICATION_X_JSON_STREAM;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.http.client.config.RequestConfig;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.AsyncClientHttpRequestFactory;
import org.zalando.problem.ThrowableProblem;
import org.zalando.riptide.NoRouteException;

import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.github.restdriver.clientdriver.ClientDriverRule;
import com.google.common.net.MediaType;

@net.jcip.annotations.NotThreadSafe
public class NakadiGatewayTest {

    private static final Logger LOG = LoggerFactory.getLogger(NakadiGatewayTest.class);

    private static final String STREAM_NAME = "stream-name";
    private static final String EVENT_TYPE = "event-type";

    private static final Event[] EVENTS = {
            EventTest.randomEvent("type-0", "value-0")
    };

    private static final Subscription SUBSCRIPTION =
            new Subscription("owner", "group", singletonList(EVENT_TYPE), "begin");

    private static final String[] DEFAULT_CURSORS_LIST = {
            "[{\"partition\":\"0\",\"offset\":\"0\"}]",
            "[{\"partition\":\"0\",\"offset\":\"1\"}]",
            "[{\"partition\":\"0\",\"offset\":\"2\"}]",
            "[{\"partition\":\"0\",\"offset\":\"3\"}]",
            "[{\"partition\":\"0\",\"offset\":\"4\"}]"
    };

    private static final String SUBSCRIPTION_RESPONSE = "{\"id\":\"" + STREAM_NAME + "\"," +
            "\"owning_application\":\"owner\",\"consumer_group\":\"group\"," +
            "\"event_types\":[\"event-type\"],\"read_from\":\"begin\"," +
            "\"created_at\":\"2016-12-24T18:59:30.123456\"}";

    private static final String PROBLEM_RESPONSE = "{\"type\":\"unknown\",\"title\":\"title\"," +
            "\"status\":500,\"detail\":\"internal\",\"instance\":\"commit\"}";

    private static final String INVALID_BATCH_RESPONSE = "{\"unknown\":\"unknown\"}";

    private static final String BATCH_RESPONSE = null;

    @Rule
    public final ClientDriverRule driver = new ClientDriverRule();

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Before
    public void before() {
        driver.reset();
    }

    @After
    public void after() {
        driver.reset();
    }

    private EventConsumer failure() {
        return event -> {
            Assert.fail();
        };
    }

    private EventConsumer consume() {
        return event -> LOG.info("consumed event: {}", event);
    }

    private NakadiGateway createGateway() {
        return new MockSetup(driver).getNakadi();
    }

    private NakadiGateway createGatewayWithTimeOut(int sotimeout) {
        final RequestConfig config = RequestConfig.copy(defaultConfig()).setSocketTimeout(sotimeout).build();
        return new MockSetup(driver, defaultConverters(), defaultFactory(defaultClient(config, 2))).getNakadi();
    }

    private NakadiGateway createGatewayWithAsyncFactory(final AsyncClientHttpRequestFactory factory) {
        return new MockSetup(driver, defaultConverters(), factory).getNakadi();
    }

    private void setupDefaultSubscribe() {
        driver.addExpectation(onRequestTo("/subscriptions").withMethod(POST),
                giveResponse(SUBSCRIPTION_RESPONSE, APPLICATION_JSON.toString()));
    }

    private void setupTimeoutSubscribe() {
        driver.addExpectation(onRequestTo("/subscriptions").withMethod(POST),
                giveResponse(SUBSCRIPTION_RESPONSE, APPLICATION_JSON.toString())
                        .after(500, MILLISECONDS));
    }

    private void setupDefaultStream() throws IOException {
        driver.addExpectation(onRequestTo("/subscriptions/" + STREAM_NAME + "/events").withMethod(GET),
                giveResponseAsBytes(getResource("event-stream.json").openStream(),
                        APPLICATION_X_JSON_STREAM.toString())
                                .withHeader(NakadiGateway.SESSION_HEADER, UUID.randomUUID().toString()));
    }

    private void setupTimeoutStream() throws IOException {
        driver.addExpectation(onRequestTo("/subscriptions/" + STREAM_NAME + "/events").withMethod(GET),
                giveResponseAsBytes(getResource("event-stream.json").openStream(),
                        APPLICATION_X_JSON_STREAM.toString())
                                .withHeader(NakadiGateway.SESSION_HEADER, UUID.randomUUID().toString())
                                .after(500, MILLISECONDS));
    }

    private void setupDefaultCoursor() {
        for (String cursors : DEFAULT_CURSORS_LIST) {
            driver.addExpectation(onRequestTo("/subscriptions/" + STREAM_NAME + "/cursors")
                    .withMethod(PUT).withBody(cursors, APPLICATION_JSON.toString()),
                    giveResponse(cursors, APPLICATION_JSON.toString()));
        }
    }

    private void setupTimeoutCursor() {
        driver.addExpectation(onRequestTo("/subscriptions/" + STREAM_NAME + "/cursors")
                .withMethod(PUT).withBody(DEFAULT_CURSORS_LIST[0], APPLICATION_JSON.toString()),
                giveResponse(DEFAULT_CURSORS_LIST[0], APPLICATION_JSON.toString())
                        .after(500, MILLISECONDS));
    }

    @Test
    public void shouldPublishEventBatches() throws Throwable {
        driver.addExpectation(onRequestTo("/event-types/" + EVENT_TYPE + "/events").withMethod(POST),
                giveResponse(BATCH_RESPONSE, APPLICATION_JSON.toString()));

        createGateway().publish(EVENT_TYPE, EVENTS);
    }

    @Test
    public void shouldExposeSubscribeIOException() throws Throwable {
        exception.expect(NakadiException.class);
        exception.expectCause(instanceOf(IOException.class));

        final AsyncClientHttpRequestFactory factory = Mockito.spy(defaultFactory());

        doThrow(new IOException()).when(factory)
                .createAsyncRequest(any(URI.class), any(HttpMethod.class));

        createGatewayWithAsyncFactory(factory).subscribe(SUBSCRIPTION);
    }

    @Test
    public void shouldExposeSubscribeFailureProblem() throws Throwable {
        exception.expect(NakadiException.class);
        exception.expectCause(instanceOf(IOException.class));
        exception.expectCause(hasFeature(Throwable::getCause, instanceOf(ThrowableProblem.class)));

        driver.addExpectation(onRequestTo("/subscriptions").withMethod(POST),
                giveResponse(PROBLEM_RESPONSE, PROBLEM.toString()).withStatus(500));

        createGateway().subscribe(SUBSCRIPTION);
    }

    @Test
    public void shouldExposeSubscribeFailureNoRoute() throws Throwable {
        exception.expect(NakadiException.class);
        exception.expectCause(instanceOf(NoRouteException.class));

        driver.addExpectation(onRequestTo("/subscriptions").withMethod(POST),
                giveResponse("failure text message", MediaType.ANY_TEXT_TYPE.toString()).withStatus(400));

        createGateway().subscribe(SUBSCRIPTION);
    }

    @Test
    public void shouldExposeSubscribeTimeoutException() throws Throwable {
        exception.expect(NakadiException.class);
        exception.expectCause(instanceOf(SocketTimeoutException.class));

        setupTimeoutSubscribe();

        createGatewayWithTimeOut(250).subscribe(SUBSCRIPTION);
    }

    @Test
    public void shouldExposeSubscribeInterruptedException() throws Throwable {
        exception.expect(NakadiException.class);
        exception.expectCause(instanceOf(InterruptedException.class));

        setupTimeoutSubscribe();

        final ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);
        final AtomicReference<Thread> thread = new AtomicReference<>();
        try {
            final NakadiGateway unit = createGateway();
            ScheduledFuture<?> future = executor.schedule(() -> {
                thread.set(Thread.currentThread());
                unit.subscribe(SUBSCRIPTION);
            }, 0, MILLISECONDS);
            executor.schedule(() -> {
                thread.get().interrupt();
            }, 100, MILLISECONDS);
            future.get();
            Assert.fail();
        } catch (ExecutionException e) {
            throw e.getCause();
        } finally {
            executor.shutdown();
        }
    }

    @Test
    public void shouldStreamEventBatches() throws Throwable {
        setupDefaultSubscribe();
        setupDefaultStream();
        setupDefaultCoursor();

        final AtomicInteger counter = new AtomicInteger();
        final NakadiGateway unit = createGateway();
        unit.stream(unit.subscribe(SUBSCRIPTION), event -> counter.incrementAndGet());

        assertThat(counter.get(), is(equalTo(6)));
    }

    @Test
    public void shouldExposeStreamIOException() throws Throwable {
        exception.expect(NakadiException.class);
        exception.expectCause(instanceOf(IOException.class));

        setupDefaultSubscribe();
        setupDefaultStream();

        final AsyncClientHttpRequestFactory factory = Mockito.spy(defaultFactory());
        final NakadiGateway unit = createGatewayWithAsyncFactory(factory);

        doCallRealMethod().doThrow(new IOException())
                .when(factory)
                .createAsyncRequest(any(URI.class), any(HttpMethod.class));

        unit.stream(unit.subscribe(SUBSCRIPTION), failure());
    }

    @Test
    public void shouldExposeStreamFailureProblem() throws Throwable {
        exception.expect(NakadiException.class);
        exception.expectCause(instanceOf(IOException.class));
        exception.expectCause(hasFeature(Throwable::getCause, instanceOf(ThrowableProblem.class)));

        setupDefaultSubscribe();
        driver.addExpectation(onRequestTo("/subscriptions/" + STREAM_NAME + "/events").withMethod(GET),
                giveResponse(PROBLEM_RESPONSE, PROBLEM.toString()).withStatus(500));

        final NakadiGateway unit = createGateway();
        unit.stream(unit.subscribe(SUBSCRIPTION), failure());
    }

    @Test
    public void shouldExposeStreamFailureNoRoute() throws Throwable {
        exception.expect(NakadiException.class);
        exception.expectCause(instanceOf(NoRouteException.class));

        setupDefaultSubscribe();
        driver.addExpectation(onRequestTo("/subscriptions/" + STREAM_NAME + "/events").withMethod(GET),
                giveResponse("failure text message", MediaType.ANY_TEXT_TYPE.toString()).withStatus(400));

        final NakadiGateway unit = createGateway();
        unit.stream(unit.subscribe(SUBSCRIPTION), failure());
    }

    @Test
    public void shouldExposeStreamTimeoutException() throws Throwable {
        exception.expect(NakadiException.class);
        exception.expectCause(instanceOf(SocketTimeoutException.class));

        setupDefaultSubscribe();
        setupTimeoutStream();

        final NakadiGateway unit = createGatewayWithTimeOut(250);
        unit.stream(unit.subscribe(SUBSCRIPTION), consume());
    }

    @Test
    public void shouldExposeStreamInterruptedException() throws Throwable {
        exception.expect(NakadiException.class);
        exception.expectCause(instanceOf(InterruptedException.class));

        setupDefaultSubscribe();
        setupTimeoutStream();

        final ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);
        final AtomicReference<Thread> thread = new AtomicReference<>();
        try {
            final NakadiGateway unit = createGateway();
            final Subscription subscribe = unit.subscribe(SUBSCRIPTION);
            ScheduledFuture<?> future = executor.schedule(() -> {
                thread.set(Thread.currentThread());
                unit.stream(subscribe, failure());
            }, 0, MILLISECONDS);
            executor.schedule(() -> {
                thread.get().interrupt();
            }, 100, MILLISECONDS);
            future.get();
            Assert.fail();
        } catch (ExecutionException e) {
            throw e.getCause();
        } finally {
            executor.shutdown();
        }
    }

    @Test
    public void shouldExposeStreamWithInvalidBatch() throws Throwable {
        exception.expect(NakadiException.class);
        exception.expectCause(instanceOf(UncheckedIOException.class));
        exception.expectCause(hasCause(instanceOf(UnrecognizedPropertyException.class)));

        setupDefaultSubscribe();
        driver.addExpectation(onRequestTo("/subscriptions/" + STREAM_NAME + "/events").withMethod(GET),
                giveResponse(INVALID_BATCH_RESPONSE, APPLICATION_X_JSON_STREAM.toString())
                        .withHeader(NakadiGateway.SESSION_HEADER, UUID.randomUUID().toString()));

        final NakadiGateway unit = createGateway();
        unit.stream(unit.subscribe(SUBSCRIPTION), event -> {
            throw new UnsupportedOperationException();
        });
    }

    @Test
    public void shouldExposeStreamConsumerException() throws Throwable {
        exception.expect(RuntimeException.class);

        setupDefaultSubscribe();
        setupDefaultStream();
        setupDefaultCoursor();

        final NakadiGateway unit = createGateway();
        unit.stream(unit.subscribe(SUBSCRIPTION), event -> {
            throw new RuntimeException();
        });
    }

    @Test
    public void shouldExposeCommitIOException() throws Throwable {
        exception.expect(NakadiException.class);
        exception.expectCause(instanceOf(IOException.class));

        setupDefaultSubscribe();
        setupDefaultStream();

        final String cursors = DEFAULT_CURSORS_LIST[0];
        driver.addExpectation(onRequestTo("/subscriptions/" + STREAM_NAME + "/cursors")
                .withMethod(PUT).withBody(cursors, APPLICATION_JSON.toString()),
                giveResponse(cursors, APPLICATION_JSON.toString()));

        final AsyncClientHttpRequestFactory factory = Mockito.spy(defaultFactory());
        final NakadiGateway unit = createGatewayWithAsyncFactory(factory);

        doCallRealMethod().doCallRealMethod()
                .doThrow(new IOException())
                .when(factory)
                .createAsyncRequest(any(URI.class), any(HttpMethod.class));

        unit.stream(unit.subscribe(SUBSCRIPTION), consume());
    }

    @Test
    public void shouldExposeCommitFailureProblem() throws Throwable {
        exception.expect(NakadiException.class);
        exception.expectCause(instanceOf(IOException.class));
        exception.expectCause(hasFeature(Throwable::getCause, instanceOf(ThrowableProblem.class)));

        setupDefaultSubscribe();
        setupDefaultStream();
        driver.addExpectation(onRequestTo("/subscriptions/" + STREAM_NAME + "/cursors").withMethod(PUT),
                giveResponse(PROBLEM_RESPONSE, PROBLEM.toString()).withStatus(500));

        final NakadiGateway unit = createGateway();
        unit.stream(unit.subscribe(SUBSCRIPTION), consume());
    }

    @Test
    public void shouldExposeCommitFailureNoRoute() throws Throwable {
        exception.expect(NakadiException.class);
        exception.expectCause(instanceOf(NoRouteException.class));

        setupDefaultSubscribe();
        setupDefaultStream();
        driver.addExpectation(onRequestTo("/subscriptions/" + STREAM_NAME + "/cursors").withMethod(PUT),
                giveResponse("failure text message", MediaType.ANY_TEXT_TYPE.toString()).withStatus(400));

        final NakadiGateway unit = createGateway();
        unit.stream(unit.subscribe(SUBSCRIPTION), consume());
    }

    @Test
    public void shouldExposeCommitTimeoutException() throws Throwable {
        exception.expect(NakadiException.class);
        exception.expectCause(instanceOf(SocketTimeoutException.class));

        setupDefaultSubscribe();
        setupDefaultStream();
        setupTimeoutCursor();

        final RequestConfig config = RequestConfig.copy(defaultConfig()).setSocketTimeout(500).build();
        final MockSetup setup = new MockSetup(driver, defaultConverters(), defaultFactory(defaultClient(config, 2)));
        final NakadiGateway unit = setup.getNakadi();

        unit.stream(unit.subscribe(SUBSCRIPTION), consume());
    }

    @Test
    public void shouldExposeCommitInterruptedException() throws Throwable {
        exception.expect(NakadiException.class);
        exception.expectCause(instanceOf(InterruptedException.class));

        setupDefaultSubscribe();
        setupDefaultStream();
        setupTimeoutCursor();

        final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        try {
            final NakadiGateway unit = createGateway();
            unit.stream(unit.subscribe(SUBSCRIPTION), event -> {
                final Thread thread = Thread.currentThread();
                executor.schedule(thread::interrupt, 0, TimeUnit.MILLISECONDS);
                LOG.info("consumed event: {}", event);
            });
        } finally {
            executor.shutdown();
        }
    }

    @Test
    // NOTE: does only validate problem of pending stream correctly when using
    // com.github.rest-driver/rest-client-driver:2.0.1-SNAPSHOT build from
    // https://github.com/tkrop/rest-driver/tree/feature/stream-and-lamda-support
    // actual test should move to riptide-httpclient!
    public void shouldAbortConnectionOnException() throws Throwable {
        exception.expect(RuntimeException.class);

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
            setupDefaultSubscribe();
            setupDefaultStream();
            setupDefaultCoursor();

            final AtomicInteger counter = new AtomicInteger();
            final NakadiGateway unit = createGateway();
            unit.stream(unit.subscribe(SUBSCRIPTION), event -> {
                if (counter.incrementAndGet() == 5) {
                    throw new RuntimeException();
                }
            });
        }
    }

}

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

import static org.springframework.http.HttpStatus.MULTI_STATUS;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;
import static org.springframework.http.HttpStatus.Series.SUCCESSFUL;
import static org.springframework.http.HttpStatus.Series.CLIENT_ERROR;
import static org.springframework.http.MediaType.parseMediaType;
import static org.zalando.riptide.Bindings.anySeries;
import static org.zalando.riptide.Bindings.on;
import static org.zalando.riptide.Navigators.contentType;
import static org.zalando.riptide.Navigators.series;
import static org.zalando.riptide.Navigators.status;
import static org.zalando.riptide.Route.listOf;
import static org.zalando.riptide.Route.noRoute;
import static org.zalando.riptide.Route.pass;
import static org.zalando.riptide.Route.propagate;
import static org.zalando.riptide.Route.responseEntityOf;
import static org.zalando.riptide.RoutingTree.dispatch;
import static org.zalando.riptide.stream.Streams.streamOf;
import static org.zalando.riptide.tryit.TryWith.tryWith;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.zalando.problem.ThrowableProblem;
import org.zalando.riptide.Binding;
import org.zalando.riptide.Bindings;
import org.zalando.riptide.Rest;
import org.zalando.riptide.ThrowingConsumer;
import org.zalando.riptide.capture.Capture;

public final class NakadiGateway {

    static final String SESSION_HEADER = "X-Nakadi-SessionId";
    public static final MediaType PROBLEM = parseMediaType("application/problem+json");

    private static final Logger LOG = LoggerFactory.getLogger(NakadiGateway.class);

    private static final Binding<HttpStatus.Series> ON_FAILURE_BINDING =
            anySeries().call(dispatch(contentType(),
                    on(PROBLEM).call(ThrowableProblem.class, propagate()),
                    Bindings.anyContentType().call(noRoute())));

    private final Rest rest;

    public NakadiGateway(final Rest rest) {
        this.rest = rest;
    }

    public List<Response> publish(final String type, final Event... events) {
        try {
            final Capture<List<Response>> capture = Capture.empty();
            rest.post("/event-types/{type}/events", type)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(events)
                    .dispatch(series(),
                            on(SUCCESSFUL).dispatch(status(),
                                    on(OK).call(pass()),
                                    on(MULTI_STATUS).call(listOf(Response.class), capture)),
                            on(CLIENT_ERROR).dispatch(status(),
                                    on(UNPROCESSABLE_ENTITY).call(listOf(Response.class), capture)),
                            ON_FAILURE_BINDING)
                    .get();
            return capture.retrieve();
        } catch (ExecutionException e) {
            throw new NakadiException(e.getCause(), FailureHandling.ABORT);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new NakadiException(e, FailureHandling.ABORT);
        } catch (IOException e) {
            throw new NakadiException(e, FailureHandling.RETRY);
        }

    }

    public Subscription subscribe(final Subscription subscription) {
        try {
            final Capture<Subscription> capture = Capture.empty();
            rest.post("/subscriptions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(subscription)
                    .dispatch(series(),
                            on(SUCCESSFUL).call(Subscription.class, capture),
                            ON_FAILURE_BINDING)
                    .get();
            return capture.retrieve();
        } catch (final ExecutionException e) {
            throw new NakadiException(e.getCause(), FailureHandling.ABORT);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new NakadiException(e, FailureHandling.ABORT);
        } catch (final IOException e) {
            throw new NakadiException(e, FailureHandling.RETRY);
        }
    }

    // TODO: add query parameter object
    // https://raw.githubusercontent.com/zalando/nakadi/master/api/nakadi-event-bus-api.yaml
    public void stream(final Subscription subscription, final EventConsumer consumer) {
        Objects.requireNonNull(subscription.getId(), "subscription identifier must not be null!");
        try {
            rest.get("/subscriptions/{id}/events", subscription.getId())
                    .dispatch(series(),
                            on(SUCCESSFUL).call(responseEntityOf(streamOf(Batch.class)),
                                    process(subscription, consumer)),
                            ON_FAILURE_BINDING)
                    .get();
        } catch (final ExecutionException e) {
            if (e.getCause() instanceof NakadiException) {
                throw (NakadiException) e.getCause();
            }
            throw new NakadiException(e.getCause(), FailureHandling.IGNORE);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new NakadiException(e, FailureHandling.ABORT);
        } catch (final IOException e) {
            throw new NakadiException(e, FailureHandling.RETRY);
        }
    }

    private ThrowingConsumer<ResponseEntity<Stream<Batch>>> process(final Subscription subscription,
            final EventConsumer consumer) {
        return entity -> {
            final Stream<Batch> stream = entity.getBody();
            final HttpHeaders headers = entity.getHeaders();
            tryWith(stream, () -> {
                stream.map(batch -> consume(consumer, batch))
                        .filter(Objects::nonNull)
                        .forEach(cursor -> commit(subscription, headers, cursor));
            });
        };
    }

    private Cursor consume(final EventConsumer consumer, final Batch batch) {
        Cursor cursor = batch.getCursor();
        if (cursor == null) {
            LOG.debug("invalid batch: [{}]", batch);
            return null;
        }
        List<Event> events = batch.getEvents();
        if (events == null || events.isEmpty()) {
            LOG.debug("empty batch: [{}]", batch);
            return null;
        }
        events.forEach(consumer::consume);
        return cursor;
    }

    private void commit(final Subscription subscription, final HttpHeaders headers, final Cursor... cursor) {
        try {
            rest.put("/subscriptions/{id}/cursors", subscription.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(SESSION_HEADER, headers.getFirst(SESSION_HEADER))
                    .body(cursor)
                    .dispatch(series(),
                            on(SUCCESSFUL).call(pass()),
                            ON_FAILURE_BINDING)
                    .get();
        } catch (final ExecutionException e) {
            throw new NakadiException(e.getCause(), FailureHandling.IGNORE);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new NakadiException(e, FailureHandling.ABORT);
        } catch (final IOException e) {
            throw new NakadiException(e, FailureHandling.IGNORE);
        }
    }
}

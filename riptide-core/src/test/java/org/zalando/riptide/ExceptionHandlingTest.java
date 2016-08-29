package org.zalando.riptide;

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

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.util.concurrent.ListenableFuture;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@RunWith(MockitoJUnitRunner.class)
public final class ExceptionHandlingTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    private final Rest unit;
    private final MockRestServiceServer server;

    @Mock
    private RoutingTree<Void> tree;

    public ExceptionHandlingTest() {
        final MockSetup setup = new MockSetup();
        this.unit = setup.getRest();
        this.server = setup.getServer();
    }

    @Before
    public void setUp() throws Exception {
        server.expect(requestTo("https://api.example.com/"))
                .andRespond(withSuccess());
    }

    @After
    public void tearDown() throws Exception {
        server.verify();
    }

    @Test
    public void shouldThrowIOExceptionWhenSettingBody() {
        try {
            unit.get("/")
                    .body("body");
        } catch (final IOException e) {
            throw new AssertionError(e);
        }
    }

    @Test
    public void shouldThrowIOExceptionWhenDispatchingWithoutBody() {
        try {
            unit.get("/")
                    .dispatch(tree);
        } catch (final IOException e) {
            throw new AssertionError(e);
        }
    }

    @Test
    public void shouldThrowInterruptedAndExecutionExceptionWhenBlocking() {
        final ListenableFuture<Void> future;

        try {
            future = unit.get("/").dispatch(tree);
        } catch (final IOException e) {
            throw new AssertionError("Request failed", e);
        }

        try {
            future.get();
        } catch (final InterruptedException e) {
            throw new AssertionError("Cancellation requested", e);
        } catch (final ExecutionException e) {
            throw new AssertionError("Response handling failed", e);
        }
    }

    @Test
    public void shouldThrowInterruptedExecutionAndTimeoutExceptionWhenBlocking() {
        final ListenableFuture<Void> future;

        try {
            future = unit.get("/").dispatch(tree);
        } catch (final IOException e) {
            throw new AssertionError("Request failed", e);
        }

        try {
            future.get(10, SECONDS);
        } catch (final InterruptedException e) {
            throw new AssertionError("Cancellation requested", e);
        } catch (final ExecutionException e) {
            throw new AssertionError("Response handling failed", e);
        } catch (final TimeoutException e) {
            throw new AssertionError("Response handling took too long", e);
        }
    }

}

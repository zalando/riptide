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
        unit.get("/")
                .body("body");
    }

    @Test
    public void shouldThrowIOExceptionWhenDispatchingWithoutBody() {
        unit.get("/")
                .dispatch(tree);
    }

    @Test
    public void shouldThrowInterruptedAndExecutionExceptionWhenBlocking() {
        unit.get("/").dispatch(tree).join();
    }

    @Test
    public void shouldThrowInterruptedExecutionAndTimeoutExceptionWhenBlocking() throws InterruptedException,
            ExecutionException, TimeoutException {

        unit.get("/").dispatch(tree).get(10, SECONDS);
    }

}

package org.zalando.riptide;

/*
 * ⁣​
 * Riptide: Core
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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(Parameterized.class)
public final class CompletionAdapterFutureTest {

    @SuppressWarnings("unchecked")
    private final CompletableFuture<Void> delegate = mock(CompletableFuture.class);
    private final Completion<Void> unit = Completion.valueOf(delegate);

    private final Tester tester;

    public CompletionAdapterFutureTest(final Tester tester) {
        this.tester = tester;
    }

    @FunctionalInterface
    private interface Tester extends ThrowingConsumer<Future<Void>> {

    }

    @Parameters(name = "{index}")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Tester[][]{
                {future -> future.cancel(true)},
                {Future::isCancelled},
                {Future::isDone},
                {Future::get},
                {future -> future.get(10, TimeUnit.SECONDS)}
        });
    }

    @Test
    public void shouldDelegate() throws Exception {
        tester.accept(unit);
        tester.accept(verify(delegate));
    }

}
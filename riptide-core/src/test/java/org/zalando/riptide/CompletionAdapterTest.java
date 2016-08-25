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

import java.util.concurrent.CompletableFuture;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public final class CompletionAdapterTest {

    @SuppressWarnings("unchecked")
    private final CompletableFuture<Void> future = mock(CompletableFuture.class);

    private final Completion<Void> unit = Completion.valueOf(future);

    @Test
    public void shouldReturnDelegateAsCompletableFuture() {
        assertThat(unit.toCompletableFuture(), is(sameInstance(future)));
    }

    @Test
    public void shouldDelegateJoin() {
        unit.join();
        verify(future).join();
    }

    @Test
    public void shouldDelegateGetNow() {
        unit.getNow(null);
        verify(future).getNow(null);
    }

}
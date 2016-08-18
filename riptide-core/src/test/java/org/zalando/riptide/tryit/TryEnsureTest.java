package org.zalando.riptide.tryit;

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

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.zalando.riptide.tryit.TryEnsure.ensureIOException;

import java.io.IOException;
import java.nio.charset.CharacterCodingException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.zalando.riptide.ThrowingRunnable;
import org.zalando.riptide.ThrowingSupplier;

public class TryEnsureTest {

    private static final Object VALUE = new Object();
    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void runnableShouldRun() throws Exception {
        ThrowingRunnable runnable = mock(ThrowingRunnable.class);

        doNothing().when(runnable).run();

        ensureIOException(runnable);

        verify(runnable, times(1)).run();
    }

    @Test
    public void runnableShouldCastException() throws Exception {
        this.exception.expect(IOException.class);
        this.exception.expectCause(instanceOf(Exception.class));

        ThrowingRunnable runnable = mock(ThrowingRunnable.class);

        doThrow(new Exception()).when(runnable).run();
        try {
            ensureIOException(runnable);
        } finally {
            verify(runnable, times(1)).run();
        }
    }

    @Test
    public void runnableShouldNotCastIOException() throws Exception {
        this.exception.expect(CharacterCodingException.class);

        ThrowingRunnable runnable = mock(ThrowingRunnable.class);

        doThrow(new CharacterCodingException()).when(runnable).run();
        try {
            ensureIOException(runnable);
        } finally {
            verify(runnable, times(1)).run();
        }
    }

    @Test
    public void supplierShouldReturnValue() throws Exception {
        ThrowingSupplier<?> supplier = mock(ThrowingSupplier.class);

        doReturn(VALUE).when(supplier).get();

        assertThat(ensureIOException(supplier), is(equalTo(VALUE)));

        verify(supplier, times(1)).get();
    }

    @Test
    public void supplierShouldCastException() throws Exception {
        this.exception.expect(IOException.class);
        this.exception.expectCause(instanceOf(Exception.class));

        ThrowingSupplier<?> supplier = mock(ThrowingSupplier.class);

        doThrow(new Exception()).when(supplier).get();
        try {
            ensureIOException(supplier);
        } finally {
            verify(supplier, times(1)).get();
        }
    }

    @Test
    public void supplierShouldNotCastIOException() throws Exception {
        this.exception.expect(CharacterCodingException.class);

        ThrowingSupplier<?> supplier = mock(ThrowingSupplier.class);

        doThrow(new CharacterCodingException()).when(supplier).get();
        try {
            ensureIOException(supplier);
        } finally {
            verify(supplier, times(1)).get();
        }
    }
}

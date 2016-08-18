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
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
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
    private static final Exception EXCEPTION = new Exception();
    private static final IOException IOEXCEPTION = new IOException();


    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void runnableShouldRun() throws Exception {
        ThrowingRunnable runnable = mock(ThrowingRunnable.class);

        doNothing().when(runnable).run();

        ensureIOException(runnable);

        verify(runnable).run();
    }

    @Test
    public void runnableShouldCastException() throws Exception {
        this.exception.expect(IOException.class);
        this.exception.expectCause(is(equalTo(EXCEPTION)));

        ThrowingRunnable runnable = mock(ThrowingRunnable.class);

        doThrow(EXCEPTION).when(runnable).run();
        try {
            ensureIOException(runnable);
        } finally {
            verify(runnable).run();
        }
    }

    @Test
    public void runnableShouldNotCastIOException() throws Exception {
        this.exception.expect(is(equalTo(IOEXCEPTION)));

        ThrowingRunnable runnable = mock(ThrowingRunnable.class);

        doThrow(IOEXCEPTION).when(runnable).run();
        try {
            ensureIOException(runnable);
        } finally {
            verify(runnable).run();
        }
    }

    @Test
    public void supplierShouldReturnValue() throws Exception {
        ThrowingSupplier<?> supplier = mock(ThrowingSupplier.class);

        doReturn(VALUE).when(supplier).get();

        assertThat(ensureIOException(supplier), is(equalTo(VALUE)));

        verify(supplier).get();
    }

    @Test
    public void supplierShouldCastException() throws Exception {
        this.exception.expect(IOException.class);
        this.exception.expectCause(is(equalTo(EXCEPTION)));

        ThrowingSupplier<?> supplier = mock(ThrowingSupplier.class);

        doThrow(EXCEPTION).when(supplier).get();
        try {
            ensureIOException(supplier);
        } finally {
            verify(supplier).get();
        }
    }

    @Test
    public void supplierShouldNotCastIOException() throws Exception {
        this.exception.expect(is(equalTo(IOEXCEPTION)));

        ThrowingSupplier<?> supplier = mock(ThrowingSupplier.class);

        doThrow(IOEXCEPTION).when(supplier).get();
        try {
            ensureIOException(supplier);
        } finally {
            verify(supplier).get();
        }
    }
}

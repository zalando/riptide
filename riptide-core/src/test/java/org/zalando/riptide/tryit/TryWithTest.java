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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.zalando.riptide.tryit.TryWith.tryWith;

import java.io.Closeable;
import java.io.IOException;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.zalando.riptide.ThrowingConsumer;
import org.zalando.riptide.ThrowingFunction;
import org.zalando.riptide.ThrowingRunnable;
import org.zalando.riptide.ThrowingSupplier;

public class TryWithTest {

    private static final Object VALUE = new Object();

    private final Exception EXCEPTION = new Exception();
    private final Exception IOEXCEPTION = new IOException();

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void shouldCloseAfterRunningWithoutException() throws Exception {
        Closeable closable = mock(Closeable.class);
        ThrowingRunnable runnable = mock(ThrowingRunnable.class);

        tryWith(closable, runnable);

        verify(runnable).run();
        verify(closable).close();
    }

    @Test
    public void shouldCloseAfterRunningWithException() throws Exception {
        exception.expect(is(equalTo(EXCEPTION)));

        Closeable closable = mock(Closeable.class);
        ThrowingRunnable runnable = mock(ThrowingRunnable.class);

        doThrow(EXCEPTION).when(runnable).run();

        try {
            tryWith(closable, runnable);
        } finally {
            verify(closable).close();
        }
    }

    @Test
    public void shouldCloseAfterRunningAndExposeExceptionWhenFailingToClose() throws Exception {
        exception.expect(is(equalTo(IOEXCEPTION)));

        Closeable closable = mock(Closeable.class);
        ThrowingRunnable runnable = mock(ThrowingRunnable.class);

        doThrow(IOEXCEPTION).when(closable).close();

        try {
            tryWith(closable, runnable);
        } finally {
            verify(closable).close();
        }
    }

    @Test
    public void shouldCloseAfterRunningAndSupressExceptionWhenFailingToClose() throws Exception {
        exception.expect(is(equalTo(EXCEPTION)));

        Closeable closable = mock(Closeable.class);
        ThrowingRunnable runnable = mock(ThrowingRunnable.class);

        doThrow(EXCEPTION).when(runnable).run();
        doThrow(IOEXCEPTION).when(closable).close();

        try {
            tryWith(closable, runnable);
        } catch (Exception ex) {
            assertThat(ex.getSuppressed().length, is(equalTo(1)));
            assertThat(ex.getSuppressed()[0], is(equalTo(IOEXCEPTION)));
            throw ex;
        }
    }

    @Test
    public void shouldCloseAfterConsumingWithoutException() throws Exception {
        Closeable closable = mock(Closeable.class);
        ThrowingConsumer<?> consumer = mock(ThrowingConsumer.class);

        tryWith(closable, consumer, null);

        verify(consumer).accept(null);
        verify(closable).close();
    }

    @Test
    public void shouldCloseAfterConsumingWithException() throws Exception {
        exception.expect(is(equalTo(EXCEPTION)));

        Closeable closable = mock(Closeable.class);
        ThrowingConsumer<?> consumer = mock(ThrowingConsumer.class);

        doThrow(EXCEPTION).when(consumer).accept(null);

        try {
            tryWith(closable, consumer, null);
        } finally {
            verify(closable).close();
        }
    }

    @Test
    public void shouldCloseAfterConsumingAndExposeExceptionWhenFailingToClose() throws Exception {
        exception.expect(is(equalTo(IOEXCEPTION)));

        Closeable closable = mock(Closeable.class);
        ThrowingConsumer<?> consumer = mock(ThrowingConsumer.class);

        doThrow(IOEXCEPTION).when(closable).close();

        try {
            tryWith(closable, consumer, null);
        } finally {
            verify(consumer).accept(null);
        }
    }

    @Test
    public void shouldCloseAfterConsumingAndSupressExceptionWhenFailingToClose() throws Exception {
        exception.expect(is(equalTo(EXCEPTION)));

        Closeable closable = mock(Closeable.class);
        ThrowingConsumer<?> consumer = mock(ThrowingConsumer.class);

        doThrow(EXCEPTION).when(consumer).accept(null);
        doThrow(IOEXCEPTION).when(closable).close();

        try {
            tryWith(closable, consumer, null);
        } catch (Exception ex) {
            assertThat(ex.getSuppressed().length, is(equalTo(1)));
            assertThat(ex.getSuppressed()[0], is(equalTo(IOEXCEPTION)));
            throw ex;
        }
    }


    @Test
    public void shouldCloseAfterProvidingWithoutException() throws Exception {
        Closeable closable = mock(Closeable.class);
        ThrowingSupplier<?> supplier = mock(ThrowingSupplier.class);

        doReturn(VALUE).when(supplier).get();

        assertThat(tryWith(closable, supplier), is(equalTo(VALUE)));

        verify(closable).close();
    }

    @Test
    public void shouldCloseAfterProvidingWithException() throws Exception {
        exception.expect(is(equalTo(EXCEPTION)));

        Closeable closable = mock(Closeable.class);
        ThrowingSupplier<?> supplier = mock(ThrowingSupplier.class);

        doThrow(EXCEPTION).when(supplier).get();

        try {
            tryWith(closable, supplier);
        } finally {
            verify(closable).close();
        }
    }

    @Test
    public void shouldCloseAfterProvidingAndExposeExceptionWhenFailingToClose() throws Exception {
        exception.expect(is(equalTo(IOEXCEPTION)));

        Closeable closable = mock(Closeable.class);
        ThrowingSupplier<?> supplier = mock(ThrowingSupplier.class);

        doReturn(VALUE).when(supplier).get();
        doThrow(IOEXCEPTION).when(closable).close();

        try {
            tryWith(closable, supplier);
        } finally {
            verify(supplier).get();
            verify(closable).close();
        }
    }

    @Test
    public void shouldCloseAfterProvidingAndSupressExceptionWhenFailingToClose() throws Exception {
        exception.expect(is(equalTo(EXCEPTION)));

        Closeable closable = mock(Closeable.class);
        ThrowingSupplier<?> supplier = mock(ThrowingSupplier.class);

        doThrow(EXCEPTION).when(supplier).get();
        doThrow(IOEXCEPTION).when(closable).close();

        try {
            tryWith(closable, supplier);
        } catch (Exception ex) {
            Assert.assertThat(ex.getSuppressed().length, is(equalTo(1)));
            Assert.assertThat(ex.getSuppressed()[0], is(equalTo(IOEXCEPTION)));
            throw ex;
        }
    }

    @Test
    public void shouldCloseAfterApplyingWithoutException() throws Exception {
        Closeable closable = mock(Closeable.class);
        ThrowingFunction<?, ?> function = mock(ThrowingFunction.class);

        doReturn(VALUE).when(function).apply(null);

        assertThat(tryWith(closable, function, null), is(equalTo(VALUE)));

        verify(closable).close();
    }

    @Test
    public void shouldCloseAfterApplyingWithException() throws Exception {
        exception.expect(is(equalTo(EXCEPTION)));

        Closeable closable = mock(Closeable.class);
        ThrowingFunction<?, ?> function = mock(ThrowingFunction.class);

        doThrow(EXCEPTION).when(function).apply(null);

        try {
            tryWith(closable, function, null);
        } finally {
            verify(closable).close();
        }
    }

    @Test
    public void shouldCloseAfterApplyingAndExposeExceptionWhenFailingToClose() throws Exception {
        exception.expect(is(equalTo(IOEXCEPTION)));

        Closeable closable = mock(Closeable.class);
        ThrowingFunction<?, ?> function = mock(ThrowingFunction.class);

        doThrow(IOEXCEPTION).when(closable).close();

        try {
            tryWith(closable, function, null);
        } finally {
            verify(function).apply(null);
        }
    }

    @Test
    public void shouldCloseAfterApplyingAndSupressExceptionWhenFailingToClose() throws Exception {
        exception.expect(is(equalTo(EXCEPTION)));

        Closeable closable = mock(Closeable.class);
        ThrowingFunction<?, ?> function = mock(ThrowingFunction.class);

        doThrow(EXCEPTION).when(function).apply(null);
        doThrow(IOEXCEPTION).when(closable).close();

        try {
            tryWith(closable, function, null);
        } catch (Exception ex) {
            assertThat(ex.getSuppressed().length, is(equalTo(1)));
            assertThat(ex.getSuppressed()[0], is(equalTo(IOEXCEPTION)));
            throw ex;
        }
    }
}

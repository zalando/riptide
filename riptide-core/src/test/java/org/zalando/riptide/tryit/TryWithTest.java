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
import static org.mockito.Mockito.verify;
import static org.zalando.riptide.tryit.TryWith.tryWith;

import java.io.Closeable;
import java.nio.charset.CharacterCodingException;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.zalando.riptide.ThrowingConsumer;
import org.zalando.riptide.ThrowingFunction;
import org.zalando.riptide.ThrowingRunnable;
import org.zalando.riptide.ThrowingSupplier;

public class TryWithTest {

    private static final Object VALUE = new Object();

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void shouldCloseAfterRunningWithoutException() throws Exception {
        Closeable closable = mock(Closeable.class);
        ThrowingRunnable runnable = mock(ThrowingRunnable.class);

        doNothing().when(runnable).run();
        doNothing().when(closable).close();

        tryWith(closable, runnable);

        verify(runnable).run();
        verify(closable).close();
    }

    @Test
    public void shouldCloseAfterRunningWithException() throws Exception {
        exception.expect(IllegalStateException.class);

        Closeable closable = mock(Closeable.class);
        ThrowingRunnable runnable = mock(ThrowingRunnable.class);

        doThrow(new IllegalStateException()).when(runnable).run();
        doNothing().when(closable).close();

        try {
            tryWith(closable, runnable);
        } finally {
            verify(runnable).run();
            verify(closable).close();
        }
    }

    @Test
    public void shouldCloseAfterRunningAndExposeExceptionWhenFailintToClose() throws Exception {
        exception.expect(CharacterCodingException.class);

        Closeable closable = mock(Closeable.class);
        ThrowingRunnable runnable = mock(ThrowingRunnable.class);

        doNothing().when(runnable).run();
        doThrow(new CharacterCodingException()).when(closable).close();

        try {
            tryWith(closable, runnable);
        } finally {
            verify(runnable).run();
            verify(closable).close();
        }
    }

    @Test
    public void shouldCloseAfterRunningAndSupressExceptionWhenFailintToClose() throws Exception {
        exception.expect(IllegalStateException.class);

        Closeable closable = mock(Closeable.class);
        ThrowingRunnable runnable = mock(ThrowingRunnable.class);

        doThrow(new IllegalStateException()).when(runnable).run();
        doThrow(new CharacterCodingException()).when(closable).close();

        try {
            tryWith(closable, runnable);
        } catch (Exception ex) {
            assertThat(ex.getSuppressed().length, is(equalTo(1)));
            assertThat(ex.getSuppressed()[0], is(instanceOf(CharacterCodingException.class)));
            throw ex;
        } finally {
            verify(runnable).run();
            verify(closable).close();
        }
    }

    @Test
    public void shouldCloseAfterConsumingWithoutException() throws Exception {
        Closeable closable = mock(Closeable.class);
        ThrowingConsumer<?> consumer = mock(ThrowingConsumer.class);

        doNothing().when(consumer).accept(null);
        doNothing().when(closable).close();

        tryWith(closable, consumer, null);

        verify(consumer).accept(null);
        verify(closable).close();
    }

    @Test
    public void shouldCloseAfterConsumingWithException() throws Exception {
        exception.expect(IllegalStateException.class);

        Closeable closable = mock(Closeable.class);
        ThrowingConsumer<?> consumer = mock(ThrowingConsumer.class);

        doThrow(new IllegalStateException()).when(consumer).accept(null);
        doNothing().when(closable).close();

        try {
            tryWith(closable, consumer, null);
        } finally {
            verify(consumer).accept(null);
            verify(closable).close();
        }
    }

    @Test
    public void shouldCloseAfterConsumingAndExposeExceptionWhenFailintToClose() throws Exception {
        exception.expect(CharacterCodingException.class);

        Closeable closable = mock(Closeable.class);
        ThrowingConsumer<?> consumer = mock(ThrowingConsumer.class);

        doNothing().when(consumer).accept(null);
        doThrow(new CharacterCodingException()).when(closable).close();

        try {
            tryWith(closable, consumer, null);
        } finally {
            verify(consumer).accept(null);
            verify(closable).close();
        }
    }

    @Test
    public void shouldCloseAfterConsumingAndSupressExceptionWhenFailintToClose() throws Exception {
        exception.expect(IllegalStateException.class);

        Closeable closable = mock(Closeable.class);
        ThrowingConsumer<?> consumer = mock(ThrowingConsumer.class);

        doThrow(new IllegalStateException()).when(consumer).accept(null);
        doThrow(new CharacterCodingException()).when(closable).close();

        try {
            tryWith(closable, consumer, null);
        } catch (Exception ex) {
            assertThat(ex.getSuppressed().length, is(equalTo(1)));
            assertThat(ex.getSuppressed()[0], is(instanceOf(CharacterCodingException.class)));
            throw ex;
        } finally {
            verify(consumer).accept(null);
            verify(closable).close();
        }
    }


    @Test
    public void shouldCloseAfterProvidingWithoutException() throws Exception {
        Closeable closable = mock(Closeable.class);
        ThrowingSupplier<?> supplier = mock(ThrowingSupplier.class);

        doReturn(VALUE).when(supplier).get();
        doNothing().when(closable).close();

        assertThat(tryWith(closable, supplier), is(equalTo(VALUE)));

        verify(supplier).get();
        verify(closable).close();
    }

    @Test
    public void shouldCloseAfterProvidingWithException() throws Exception {
        exception.expect(IllegalStateException.class);

        Closeable closable = mock(Closeable.class);
        ThrowingSupplier<?> supplier = mock(ThrowingSupplier.class);

        doThrow(new IllegalStateException()).when(supplier).get();
        doNothing().when(closable).close();

        try {
            tryWith(closable, supplier);
        } finally {
            verify(supplier).get();
            verify(closable).close();
        }
    }

    @Test
    public void shouldCloseAfterProvidingAndExposeExceptionWhenFailintToClose() throws Exception {
        exception.expect(CharacterCodingException.class);

        Closeable closable = mock(Closeable.class);
        ThrowingSupplier<?> supplier = mock(ThrowingSupplier.class);

        doReturn(VALUE).when(supplier).get();
        doThrow(new CharacterCodingException()).when(closable).close();

        try {
            tryWith(closable, supplier);
        } finally {
            verify(supplier).get();
            verify(closable).close();
        }
    }

    @Test
    public void shouldCloseAfterProvidingAndSupressExceptionWhenFailintToClose() throws Exception {
        exception.expect(IllegalStateException.class);

        Closeable closable = mock(Closeable.class);
        ThrowingSupplier<?> supplier = mock(ThrowingSupplier.class);

        doThrow(new IllegalStateException()).when(supplier).get();
        doThrow(new CharacterCodingException()).when(closable).close();

        try {
            tryWith(closable, supplier);
        } catch (Exception ex) {
            Assert.assertThat(ex.getSuppressed().length, is(equalTo(1)));
            Assert.assertThat(ex.getSuppressed()[0], is(instanceOf(CharacterCodingException.class)));
            throw ex;
        } finally {
            Mockito.verify(supplier).get();
            Mockito.verify(closable).close();
        }
    }

    @Test
    public void shouldCloseAfterApplyingWithoutException() throws Exception {
        Closeable closable = mock(Closeable.class);
        ThrowingFunction<?, ?> function = mock(ThrowingFunction.class);

        doReturn(VALUE).when(function).apply(null);
        doNothing().when(closable).close();

        assertThat(tryWith(closable, function, null), is(equalTo(VALUE)));

        verify(function).apply(null);
        verify(closable).close();
    }

    @Test
    public void shouldCloseAfterApplyingWithException() throws Exception {
        exception.expect(IllegalStateException.class);

        Closeable closable = mock(Closeable.class);
        ThrowingFunction<?, ?> function = mock(ThrowingFunction.class);

        doThrow(new IllegalStateException()).when(function).apply(null);
        doNothing().when(closable).close();

        try {
            tryWith(closable, function, null);
        } finally {
            verify(function).apply(null);
            verify(closable).close();
        }
    }

    @Test
    public void shouldCloseAfterApplyingAndExposeExceptionWhenFailintToClose() throws Exception {
        exception.expect(CharacterCodingException.class);

        Closeable closable = mock(Closeable.class);
        ThrowingFunction<?, ?> function = mock(ThrowingFunction.class);

        doReturn(null).when(function).apply(null);
        doThrow(new CharacterCodingException()).when(closable).close();

        try {
            tryWith(closable, function, null);
        } finally {
            verify(function).apply(null);
            verify(closable).close();
        }
    }

    @Test
    public void shouldCloseAfterApplyingAndSupressExceptionWhenFailintToClose() throws Exception {
        exception.expect(IllegalStateException.class);

        Closeable closable = mock(Closeable.class);
        ThrowingFunction<?, ?> function = mock(ThrowingFunction.class);

        doThrow(new IllegalStateException()).when(function).apply(null);
        doThrow(new CharacterCodingException()).when(closable).close();

        try {
            tryWith(closable, function, null);
        } catch (Exception ex) {
            assertThat(ex.getSuppressed().length, is(equalTo(1)));
            assertThat(ex.getSuppressed()[0], is(instanceOf(CharacterCodingException.class)));
            throw ex;
        } finally {
            verify(function).apply(null);
            verify(closable).close();
        }
    }
}

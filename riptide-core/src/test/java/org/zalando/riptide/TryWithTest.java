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

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.zalando.riptide.TryWith.tryWith;

import java.io.Closeable;
import java.nio.charset.CharacterCodingException;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

public class TryWithTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void shouldCloseAfterRunningWithoutException() throws Exception {
        Closeable closable = mock(Closeable.class);
        ThrowingRunnable consumer = mock(ThrowingRunnable.class);

        Mockito.doNothing().when(consumer).run();
        Mockito.doNothing().when(closable).close();

        tryWith(closable, consumer);

        Mockito.verify(consumer, times(1)).run();
        Mockito.verify(closable, times(1)).close();
    }

    @Test
    public void shouldCloseAfterRunningWithException() throws Exception {
        exception.expect(IllegalStateException.class);

        Closeable closable = mock(Closeable.class);
        ThrowingRunnable consumer = mock(ThrowingRunnable.class);

        Mockito.doThrow(new IllegalStateException()).when(consumer).run();
        Mockito.doNothing().when(closable).close();

        try {
            tryWith(closable, consumer);
        } finally {
            Mockito.verify(consumer, times(1)).run();
            Mockito.verify(closable, times(1)).close();
        }
    }

    @Test
    public void shouldCloseAfterRunningAndExposeExceptionWhenFailintToClose() throws Exception {
        exception.expect(CharacterCodingException.class);

        Closeable closable = mock(Closeable.class);
        ThrowingRunnable consumer = mock(ThrowingRunnable.class);

        Mockito.doNothing().when(consumer).run();
        Mockito.doThrow(new CharacterCodingException()).when(closable).close();

        try {
            tryWith(closable, consumer);
        } finally {
            Mockito.verify(consumer, times(1)).run();
            Mockito.verify(closable, times(1)).close();
        }
    }

    @Test
    public void shouldCloseAfterRunningAndSupressExceptionWhenFailintToClose() throws Exception {
        exception.expect(IllegalStateException.class);

        Closeable closable = mock(Closeable.class);
        ThrowingRunnable consumer = mock(ThrowingRunnable.class);

        Mockito.doThrow(new IllegalStateException()).when(consumer).run();
        Mockito.doThrow(new CharacterCodingException()).when(closable).close();

        try {
            tryWith(closable, consumer);
        } catch (Exception ex) {
            Assert.assertThat(ex.getSuppressed().length, is(equalTo(1)));
            Assert.assertThat(ex.getSuppressed()[0], is(instanceOf(CharacterCodingException.class)));
            throw ex;
        } finally {
            Mockito.verify(consumer, times(1)).run();
            Mockito.verify(closable, times(1)).close();
        }
    }

    @Test
    public void shouldCloseAfterAcceptingWithoutException() throws Exception {
        Closeable closable = mock(Closeable.class);
        ThrowingConsumer<?> consumer = mock(ThrowingConsumer.class);

        Mockito.doNothing().when(consumer).accept(null);
        Mockito.doNothing().when(closable).close();

        tryWith(closable, consumer, null);

        Mockito.verify(consumer, times(1)).accept(null);
        Mockito.verify(closable, times(1)).close();
    }

    @Test
    public void shouldCloseAfterAcceptingWithException() throws Exception {
        exception.expect(IllegalStateException.class);

        Closeable closable = mock(Closeable.class);
        ThrowingConsumer<?> consumer = mock(ThrowingConsumer.class);

        Mockito.doThrow(new IllegalStateException()).when(consumer).accept(null);
        Mockito.doNothing().when(closable).close();

        try {
            tryWith(closable, consumer, null);
        } finally {
            Mockito.verify(consumer, times(1)).accept(null);
            Mockito.verify(closable, times(1)).close();
        }
    }

    @Test
    public void shouldCloseAfterAcceptingAndExposeExceptionWhenFailintToClose() throws Exception {
        exception.expect(CharacterCodingException.class);

        Closeable closable = mock(Closeable.class);
        ThrowingConsumer<?> consumer = mock(ThrowingConsumer.class);

        Mockito.doNothing().when(consumer).accept(null);
        Mockito.doThrow(new CharacterCodingException()).when(closable).close();

        try {
            tryWith(closable, consumer, null);
        } finally {
            Mockito.verify(consumer, times(1)).accept(null);
            Mockito.verify(closable, times(1)).close();
        }
    }

    @Test
    public void shouldCloseAfterAcceptingAndSupressExceptionWhenFailintToClose() throws Exception {
        exception.expect(IllegalStateException.class);

        Closeable closable = mock(Closeable.class);
        ThrowingConsumer<?> consumer = mock(ThrowingConsumer.class);

        Mockito.doThrow(new IllegalStateException()).when(consumer).accept(null);
        Mockito.doThrow(new CharacterCodingException()).when(closable).close();

        try {
            tryWith(closable, consumer, null);
        } catch (Exception ex) {
            Assert.assertThat(ex.getSuppressed().length, is(equalTo(1)));
            Assert.assertThat(ex.getSuppressed()[0], is(instanceOf(CharacterCodingException.class)));
            throw ex;
        } finally {
            Mockito.verify(consumer, times(1)).accept(null);
            Mockito.verify(closable, times(1)).close();
        }
    }
}

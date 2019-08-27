package org.zalando.riptide.opentracing;

import org.junit.jupiter.api.*;

import java.io.*;
import java.util.concurrent.*;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.zalando.riptide.opentracing.OpenTracingPlugin.*;

final class UnpackTest {

    @Test
    void shouldUnpackCompletionException() {
        final IOException cause = new IOException();
        assertThat(unpack(new CompletionException(cause)), is(cause));
    }

    @Test
    void shouldNotUnpackNonCompletionException() {
        final RuntimeException exception = new RuntimeException(new IOException());
        assertThat(unpack(exception), is(exception));
    }

}

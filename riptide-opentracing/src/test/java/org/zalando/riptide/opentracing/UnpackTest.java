package org.zalando.riptide.opentracing;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.CompletionException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.zalando.riptide.opentracing.OpenTracingPlugin.unpack;

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

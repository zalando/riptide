package org.zalando.riptide.opentracing.span;

import org.junit.jupiter.api.*;
import org.zalando.riptide.opentracing.span.ErrorStackSpanDecorator.*;

import java.io.*;
import java.net.*;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

class StackRendererTest {

    private final StackRenderer unit = new StackRenderer();

    @Test
    void shouldRender() {
        final String stack = unit.render(new IOException("Timeout"));

        assertThat(stack, containsString("java.io.IOException: Timeout"));
        assertThat(stack, not(containsString("Caused by:")));
    }

    @Test
    void shouldRenderWithOriginalStackTrace() {
        final String stack = unit.render(new IOException("Timeout"), new StackTraceElement[] {
                new StackTraceElement("org.zalando.riptide.opentracing.MyClass", "test", "MyClass.java", 17)
        });

        assertThat(stack, containsString("java.io.IOException: Timeout"));
        assertThat(stack, containsString("\tat org.zalando.riptide.opentracing.MyClass.test(MyClass.java:17)"));
    }

    @Test
    void shouldRenderWithCause() {
        final String stack = unit.render(new IOException("Timeout", new SocketTimeoutException("Read timed out")));

        assertThat(stack, containsString("java.io.IOException: Timeout"));
        assertThat(stack, containsString("Caused by: java.net.SocketTimeoutException: Read timed out"));
    }

}

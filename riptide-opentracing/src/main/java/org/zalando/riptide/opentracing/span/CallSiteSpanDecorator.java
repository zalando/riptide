package org.zalando.riptide.opentracing.span;

import io.opentracing.Span;
import io.opentracing.Tracer;
import org.zalando.riptide.RequestArguments;
import org.zalando.riptide.opentracing.OpenTracingPlugin;

import java.util.Collections;

public final class CallSiteSpanDecorator implements SpanDecorator {

    @Override
    public void onStart(final Tracer.SpanBuilder builder, final RequestArguments arguments) {
        arguments.getAttribute(OpenTracingPlugin.TAGS)
                .orElseGet(Collections::emptyMap)
                .forEach(builder::withTag);
    }

    @Override
    public void onStarted(final Span span, final RequestArguments arguments) {
        arguments.getAttribute(OpenTracingPlugin.LOGS)
                .ifPresent(span::log);
    }

}

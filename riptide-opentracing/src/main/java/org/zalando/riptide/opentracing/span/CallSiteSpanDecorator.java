package org.zalando.riptide.opentracing.span;

import io.opentracing.Span;
import org.zalando.riptide.RequestArguments;
import org.zalando.riptide.opentracing.OpenTracingPlugin;

import java.util.Collections;

public final class CallSiteSpanDecorator implements SpanDecorator {

    @Override
    public void onRequest(final Span span, final RequestArguments arguments) {
        arguments.getAttribute(OpenTracingPlugin.TAGS)
                .orElseGet(Collections::emptyMap)
                .forEach(span::setTag);

        arguments.getAttribute(OpenTracingPlugin.LOGS)
                .ifPresent(span::log);
    }

}

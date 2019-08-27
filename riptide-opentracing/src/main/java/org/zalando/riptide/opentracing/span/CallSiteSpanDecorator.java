package org.zalando.riptide.opentracing.span;

import io.opentracing.*;
import org.zalando.riptide.*;
import org.zalando.riptide.opentracing.*;

import java.util.*;

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

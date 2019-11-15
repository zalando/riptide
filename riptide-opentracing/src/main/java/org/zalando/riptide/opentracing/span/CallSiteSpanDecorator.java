package org.zalando.riptide.opentracing.span;

import io.opentracing.Span;
import org.apiguardian.api.API;
import org.zalando.riptide.RequestArguments;
import org.zalando.riptide.opentracing.OpenTracingPlugin;

import java.util.Collections;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;

@API(status = EXPERIMENTAL)
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

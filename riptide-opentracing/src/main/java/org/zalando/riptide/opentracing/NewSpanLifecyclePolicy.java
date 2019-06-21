package org.zalando.riptide.opentracing;

import io.opentracing.Span;
import io.opentracing.Tracer;
import lombok.AllArgsConstructor;
import org.zalando.riptide.RequestArguments;
import org.zalando.riptide.opentracing.span.SpanDecorator;

import static org.zalando.riptide.opentracing.OpenTracingPlugin.OPERATION_NAME;

@AllArgsConstructor
final class NewSpanLifecyclePolicy implements LifecyclePolicy {

    @Override
    public Span start(final Tracer tracer, final RequestArguments arguments, final SpanDecorator decorator) {
        final String operationName = arguments.getAttribute(OPERATION_NAME)
                .orElse(arguments.getMethod().name());

        final Tracer.SpanBuilder builder = tracer.buildSpan(operationName);
        decorator.onStart(builder, arguments);
        final Span span = builder.start();
        decorator.onStarted(span, arguments);

        return span;
    }

    @Override
    public void finish(final Span span) {
        span.finish();
    }

}

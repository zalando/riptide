package org.zalando.riptide.opentracing;

import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import org.zalando.riptide.RequestArguments;

final class NoOpInjection implements Injection {

    @Override
    public RequestArguments inject(
            final Tracer tracer,
            final RequestArguments arguments,
            final SpanContext context) {

        return arguments;
    }

}

package org.zalando.riptide.opentracing;

import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import org.zalando.riptide.RequestArguments;

interface Injection {

    RequestArguments inject(Tracer tracer, RequestArguments arguments, SpanContext context);

}

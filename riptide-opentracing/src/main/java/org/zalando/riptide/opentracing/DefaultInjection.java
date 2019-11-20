package org.zalando.riptide.opentracing;

import com.google.common.collect.Multimaps;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.TextMapAdapter;
import org.zalando.riptide.RequestArguments;

import java.util.HashMap;
import java.util.Map;

import static io.opentracing.propagation.Format.Builtin.HTTP_HEADERS;

final class DefaultInjection implements Injection {

    @Override
    public RequestArguments inject(
            final Tracer tracer,
            final RequestArguments arguments,
            final SpanContext context) {

        final Map<String, String> headers = new HashMap<>();
        tracer.inject(context, HTTP_HEADERS, new TextMapAdapter(headers));
        return arguments.withHeaders(Multimaps.forMap(headers).asMap());
    }

}

package org.zalando.riptide.opentracing.span;

import io.opentracing.Span;
import lombok.AllArgsConstructor;
import org.apiguardian.api.API;
import org.zalando.riptide.RequestArguments;

import static org.apiguardian.api.API.Status.INTERNAL;

@API(status = INTERNAL)
@AllArgsConstructor
final class HttpRequestHeaderSpanDecorator implements SpanDecorator {

    private final HttpSpanOperator operator;

    @Override
    public void onRequest(final Span span, final RequestArguments arguments) {
        operator.apply(span, arguments.getHeaders());
    }

}

package org.zalando.riptide.opentracing.span;

import io.opentracing.Span;
import lombok.AllArgsConstructor;
import org.apiguardian.api.API;
import org.zalando.riptide.RequestArguments;

import java.util.Map;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;

/**
 * Sets arbitrary, static span tags.
 */
@API(status = EXPERIMENTAL)
@AllArgsConstructor
public final class StaticSpanDecorator implements SpanDecorator {

    private final Map<String, String> tags;

    @Override
    public void onRequest(final Span span, final RequestArguments arguments) {
        tags.forEach(span::setTag);
    }

}

package org.zalando.riptide.opentelemetry.span;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import org.zalando.riptide.RequestArguments;

import java.util.HashMap;
import java.util.Map;

public class StaticSpanDecorator implements SpanDecorator {
    private final Map<AttributeKey<String>, String> attributes = new HashMap<>();

    public StaticSpanDecorator(Map<String, String> source) {
        source.forEach((k, v) -> attributes.put(AttributeKey.stringKey(k), v));
    }

    @Override
    public void onRequest(Span span, RequestArguments arguments) {
        attributes.forEach(span::setAttribute);
    }
}

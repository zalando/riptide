package org.zalando.riptide.opentracing.span;

import io.opentracing.Span;

import java.util.List;
import java.util.Map;

interface HttpSpanOperator {
    void apply(Span span, Map<String, List<String>> headers);
}

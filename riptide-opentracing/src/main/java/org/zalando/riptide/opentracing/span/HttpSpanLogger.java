package org.zalando.riptide.opentracing.span;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import io.opentracing.Span;
import lombok.AllArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static lombok.AccessLevel.PRIVATE;

@AllArgsConstructor(access = PRIVATE)
final class HttpSpanLogger implements HttpSpanOperator {

    private final HeaderExtractor extractor = new HeaderExtractor();

    private final Map<String, List<String>> logs;

    @Override
    public void apply(
            final Span span,
            final Map<String, List<String>> headers) {

        final Map<String, String> fields = new HashMap<>();

        logs.forEach((log, names) ->
                extractor.extract(headers, names).ifPresent(value ->
                        fields.put(log, value)));

        if (fields.isEmpty()) {
            return;
        }

        span.log(fields);
    }

    static HttpSpanLogger logging(
            final String log,
            final String name,
            final String... names) {

        return logging(ImmutableMap.of(log, Lists.asList(name, names)));
    }

    static HttpSpanLogger logging(final Map<String, List<String>> logs) {
        return new HttpSpanLogger(logs);
    }

}

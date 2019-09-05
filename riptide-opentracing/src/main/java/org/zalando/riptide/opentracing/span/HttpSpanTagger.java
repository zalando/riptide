package org.zalando.riptide.opentracing.span;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import io.opentracing.Span;
import io.opentracing.tag.Tag;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.Map;

import static lombok.AccessLevel.PRIVATE;

@AllArgsConstructor(access = PRIVATE)
final class HttpSpanTagger implements HttpSpanOperator {

    private final HeaderExtractor extractor = new HeaderExtractor();

    private final Map<Tag<String>, List<String>> tags;

    @Override
    public void apply(
            final Span span,
            final Map<String, List<String>> headers) {

        tags.forEach((tag, names) ->
                extractor.extract(headers, names).ifPresent(value ->
                        span.setTag(tag, value)));
    }

    static HttpSpanTagger tagging(
            final Tag<String> tag,
            final String name,
            final String... names) {

        return tagging(ImmutableMap.of(tag, Lists.asList(name, names)));
    }

    static HttpSpanTagger tagging(final Map<Tag<String>, List<String>> tags) {
        return new HttpSpanTagger(tags);
    }

}

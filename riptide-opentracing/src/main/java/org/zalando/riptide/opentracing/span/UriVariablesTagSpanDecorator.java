package org.zalando.riptide.opentracing.span;

import com.google.common.collect.ImmutableMap;
import com.google.gag.annotation.remark.Hack;
import io.opentracing.Tracer.SpanBuilder;
import org.springframework.web.util.UriComponentsBuilder;
import org.zalando.riptide.RequestArguments;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * A {@link SpanDecorator decorator} that extracts contextual tags from the used
 * {@link RequestArguments#getUriTemplate() URI template} and {@link RequestArguments#getUriVariables() URI variables}.
 *
 * Using this decorator in conjunction with {@code http.get("/accounts/{account_id}", 792)}
 * will produce the tag {@code account_id=792}.
 *
 * <a href="https://opentracing.io/specification/#start-a-new-span">The OpenTracing Semantic Specification: Start a new Span</a>
 */
public final class UriVariablesTagSpanDecorator implements SpanDecorator {

    @Override
    public void onStart(final SpanBuilder builder, final RequestArguments arguments) {
        final Map<String, String> variables = extract(arguments);
        variables.forEach(builder::withTag);
    }

    private Map<String, String> extract(final RequestArguments arguments) {
        @Nullable final String template = arguments.getUriTemplate();

        if (template == null) {
            return ImmutableMap.of();
        }

        return extract(template, arguments.getUriVariables());
    }

    @Hack("Pretty dirty, but I couldn't find any other way...")
    private Map<String, String> extract(final String template, final List<Object> values) {
        final Map<String, String> variables = new HashMap<>(values.size());
        final Iterator<Object> iterator = values.iterator();

        UriComponentsBuilder.fromUriString(template).build().expand(name -> {
            final Object value = iterator.next();
            variables.put(name, String.valueOf(value));
            return value;
        });

        return variables;
    }

}

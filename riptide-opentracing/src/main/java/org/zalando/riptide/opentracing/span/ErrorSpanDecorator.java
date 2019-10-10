package org.zalando.riptide.opentracing.span;

import com.google.common.collect.ImmutableMap;
import io.opentracing.Span;
import io.opentracing.log.Fields;
import io.opentracing.tag.Tags;
import lombok.AllArgsConstructor;
import org.springframework.http.client.ClientHttpResponse;
import org.zalando.riptide.RequestArguments;

import java.io.IOException;
import java.util.function.Predicate;

import static org.zalando.fauxpas.FauxPas.throwingPredicate;

/**
 * Sets the <code>error</code> span tag as well as the <code>error.kind</code> and <code>error.object</code> span logs.
 *
 * @see <a href="https://opentracing.io/specification/conventions/#span-tags-table">Standard Span Tags</a>
 * @see <a href="https://opentracing.io/specification/conventions/#log-fields-table">Standard Log Fields</a>
 */
@AllArgsConstructor
public final class ErrorSpanDecorator implements SpanDecorator {

    private final Predicate<ClientHttpResponse> predicate;

    public ErrorSpanDecorator() {
        this(throwingPredicate(response ->
                response.getStatusCode().is5xxServerError()));
    }

    @Override
    public void onResponse(
            final Span span,
            final RequestArguments arguments,
            final ClientHttpResponse response) throws IOException {

        if (predicate.test(response)) {
            span.setTag(Tags.ERROR, true);
        }
    }

    @Override
    public void onError(final Span span, final RequestArguments arguments, final Throwable error) {
        span.setTag(Tags.ERROR, true);
        span.log(ImmutableMap.of(
                Fields.ERROR_KIND, error.getClass().getSimpleName(),
                Fields.ERROR_OBJECT, error
        ));
    }

}

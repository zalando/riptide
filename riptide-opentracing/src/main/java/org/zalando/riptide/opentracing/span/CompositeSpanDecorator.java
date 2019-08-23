package org.zalando.riptide.opentracing.span;

import com.google.common.collect.Lists;
import io.opentracing.Span;
import lombok.AllArgsConstructor;
import org.apiguardian.api.API;
import org.springframework.http.client.ClientHttpResponse;
import org.zalando.riptide.RequestArguments;

import static lombok.AccessLevel.PRIVATE;
import static org.apiguardian.api.API.Status.EXPERIMENTAL;
import static org.zalando.fauxpas.FauxPas.throwingConsumer;

@API(status = EXPERIMENTAL)
@AllArgsConstructor(access = PRIVATE)
public final class CompositeSpanDecorator implements SpanDecorator {

    private final Iterable<SpanDecorator> decorators;

    @Override
    public void onRequest(final Span span, final RequestArguments arguments) {
        decorators.forEach(decorator ->
                decorator.onRequest(span, arguments));
    }

    @Override
    public void onResponse(final Span span, final RequestArguments arguments, final ClientHttpResponse response) {
        decorators.forEach(throwingConsumer(decorator ->
                decorator.onResponse(span, arguments, response)));
    }

    @Override
    public void onError(final Span span, final RequestArguments arguments, final Throwable error) {
        decorators.forEach(decorator ->
                decorator.onError(span, arguments, error));
    }

    public static SpanDecorator composite(final SpanDecorator decorator, final SpanDecorator... decorators) {
        return composite(Lists.asList(decorator, decorators));
    }

    public static SpanDecorator composite(final Iterable<SpanDecorator> decorators) {
        return new CompositeSpanDecorator(decorators);
    }

}


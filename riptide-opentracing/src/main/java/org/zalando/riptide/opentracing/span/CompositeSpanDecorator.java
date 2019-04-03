package org.zalando.riptide.opentracing.span;

import io.opentracing.Span;
import io.opentracing.Tracer.SpanBuilder;
import lombok.Getter;
import org.springframework.http.client.ClientHttpResponse;
import org.zalando.riptide.RequestArguments;

import java.util.Collection;

import static org.zalando.fauxpas.FauxPas.throwingConsumer;

final class CompositeSpanDecorator implements SpanDecorator {

    @Getter
    private final Collection<SpanDecorator> decorators;

    CompositeSpanDecorator(final Collection<SpanDecorator> decorators) {
        this.decorators = decorators;
    }

    @Override
    public void onStart(final SpanBuilder builder, final RequestArguments arguments) {
        decorators.forEach(decorator -> decorator.onStart(builder, arguments));
    }

    @Override
    public void onStarted(final Span span, final RequestArguments arguments) {
        decorators.forEach(decorator -> decorator.onStarted(span, arguments));

    }

    @Override
    public void onResponse(final Span span, final RequestArguments arguments, final ClientHttpResponse response) {
        decorators.forEach(throwingConsumer(decorator -> {
            decorator.onResponse(span, arguments, response);
        }));
    }

    @Override
    public void onError(final Span span, final RequestArguments arguments, final Throwable error) {
        decorators.forEach(decorator -> decorator.onError(span, arguments, error));
    }

}

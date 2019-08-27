package org.zalando.riptide.opentracing.span;

import io.opentracing.*;
import lombok.*;
import org.springframework.http.client.*;
import org.zalando.riptide.*;

import java.util.*;

import static org.zalando.fauxpas.FauxPas.*;

final class CompositeSpanDecorator implements SpanDecorator {

    @Getter
    private final Collection<SpanDecorator> decorators;

    CompositeSpanDecorator(final Collection<SpanDecorator> decorators) {
        this.decorators = decorators;
    }

    @Override
    public void onRequest(final Span span, final RequestArguments arguments) {
        decorators.forEach(decorator -> decorator.onRequest(span, arguments));

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

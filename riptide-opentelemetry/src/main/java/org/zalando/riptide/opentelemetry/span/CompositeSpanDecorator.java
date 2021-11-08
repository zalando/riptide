package org.zalando.riptide.opentelemetry.span;

import io.opentelemetry.api.trace.Span;
import lombok.AllArgsConstructor;
import org.springframework.http.client.ClientHttpResponse;
import org.zalando.riptide.RequestArguments;

import java.io.IOException;
import java.util.Arrays;

import static lombok.AccessLevel.PRIVATE;
import static org.zalando.fauxpas.FauxPas.throwingConsumer;

@AllArgsConstructor(access = PRIVATE)
public class CompositeSpanDecorator implements SpanDecorator {

    private final Iterable<SpanDecorator> decorators;

    @Override
    public void onRequest(Span span, RequestArguments arguments) {
        decorators.forEach(decorator -> decorator.onRequest(span, arguments));
    }

    @Override
    public void onResponse(Span span, RequestArguments arguments,
                           ClientHttpResponse response) throws IOException {
        decorators.forEach(throwingConsumer(decorator -> decorator.onResponse(span, arguments, response)));
    }

    @Override
    public void onError(Span span, RequestArguments arguments, Throwable error) {
        decorators.forEach(decorator -> decorator.onError(span, arguments, error));
    }

    public static SpanDecorator composite(final SpanDecorator... decorators) {
        return new CompositeSpanDecorator(Arrays.asList(decorators));
    }
}

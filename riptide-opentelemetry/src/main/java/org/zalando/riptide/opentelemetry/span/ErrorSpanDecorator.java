package org.zalando.riptide.opentelemetry.span;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import lombok.AllArgsConstructor;
import org.springframework.http.client.ClientHttpResponse;
import org.zalando.riptide.RequestArguments;

import java.io.IOException;
import java.util.function.Predicate;

import static org.zalando.fauxpas.FauxPas.throwingPredicate;

@AllArgsConstructor
public class ErrorSpanDecorator implements SpanDecorator {
    private final Predicate<ClientHttpResponse> predicate;

    public ErrorSpanDecorator() {
        predicate = throwingPredicate(response -> response.getStatusCode().is5xxServerError());
    }

    @Override
    public void onResponse(
            final Span span,
            final RequestArguments arguments,
            final ClientHttpResponse response) throws IOException {
        if (predicate.test(response)) {
            span.setStatus(StatusCode.ERROR);
        }
    }

    @Override
    public void onError(final Span span, final RequestArguments arguments, final Throwable error) {
        span.setStatus(StatusCode.ERROR);
        span.recordException(error);
    }
}

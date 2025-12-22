package org.zalando.riptide.opentracing.span;

import io.opentracing.Span;
import lombok.AllArgsConstructor;
import org.apiguardian.api.API;
import org.springframework.http.client.ClientHttpResponse;
import org.zalando.riptide.RequestArguments;

import static org.apiguardian.api.API.Status.INTERNAL;

@API(status = INTERNAL)
@AllArgsConstructor
final class HttpResponseHeaderSpanDecorator implements SpanDecorator {

    private final HttpSpanOperator operator;

    @Override
    public void onResponse(
            final Span span,
            final RequestArguments arguments,
            final ClientHttpResponse response) {

        operator.apply(span, response.getHeaders().asMultiValueMap());
    }

}

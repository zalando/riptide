package org.zalando.riptide.opentracing.span;

import com.google.common.collect.Lists;
import io.opentracing.Span;
import org.apiguardian.api.API;
import org.springframework.http.client.ClientHttpResponse;
import org.zalando.riptide.RequestArguments;

import java.io.IOException;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;

@API(status = EXPERIMENTAL)
public interface SpanDecorator {

    default void onRequest(final Span span, final RequestArguments arguments) {
        // nothing to do
    }

    default void onResponse(final Span span, final RequestArguments arguments, final ClientHttpResponse response)
            throws IOException {
        // nothing to do
    }

    default void onError(final Span span, final RequestArguments arguments, final Throwable error) {
        // nothing to do
    }

}

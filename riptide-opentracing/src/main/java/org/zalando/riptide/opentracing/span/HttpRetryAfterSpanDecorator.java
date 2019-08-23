package org.zalando.riptide.opentracing.span;

import org.apiguardian.api.API;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;
import static org.springframework.http.HttpHeaders.RETRY_AFTER;
import static org.zalando.riptide.opentracing.span.HttpSpanLogger.logging;

@API(status = EXPERIMENTAL)
public final class HttpRetryAfterSpanDecorator extends ForwardingSpanDecorator {

    private static final String HTTP_RETRY_AFTER = "http.retry_after";

    public HttpRetryAfterSpanDecorator() {
        super(new HttpResponseHeaderSpanDecorator(
                logging(HTTP_RETRY_AFTER, RETRY_AFTER)));
    }

}

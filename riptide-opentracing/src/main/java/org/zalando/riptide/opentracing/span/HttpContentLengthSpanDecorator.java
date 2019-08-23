package org.zalando.riptide.opentracing.span;

import org.apiguardian.api.API;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;
import static org.springframework.http.HttpHeaders.CONTENT_LENGTH;
import static org.zalando.riptide.opentracing.span.HttpSpanLogger.logging;

@API(status = EXPERIMENTAL)
public final class HttpContentLengthSpanDecorator extends ForwardingSpanDecorator {

    private static final String HTTP_CONTENT_LENGTH = "http.content_length";

    public HttpContentLengthSpanDecorator() {
        super(new HttpResponseHeaderSpanDecorator(
                logging(HTTP_CONTENT_LENGTH, CONTENT_LENGTH)));
    }

}

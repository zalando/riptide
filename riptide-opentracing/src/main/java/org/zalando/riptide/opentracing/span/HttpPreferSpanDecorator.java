package org.zalando.riptide.opentracing.span;

import io.opentracing.tag.StringTag;
import io.opentracing.tag.Tag;
import org.apiguardian.api.API;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;
import static org.zalando.riptide.opentracing.span.HttpSpanTagger.tagging;

/**
 * @see <a href="https://tools.ietf.org/html/rfc7240">Prefer</a>
 */
@API(status = EXPERIMENTAL)
public final class HttpPreferSpanDecorator extends ForwardingSpanDecorator {

    private static final Tag<String> HTTP_PREFER =
            new StringTag("http.prefer");

    public HttpPreferSpanDecorator() {
        super(new HttpRequestHeaderSpanDecorator(tagging(HTTP_PREFER, "Prefer")));
    }

}

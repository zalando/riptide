package org.zalando.riptide.opentracing.span;

import io.opentracing.tag.StringTag;
import io.opentracing.tag.Tag;
import org.apiguardian.api.API;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;
import static org.springframework.http.HttpHeaders.CONTENT_LANGUAGE;
import static org.zalando.riptide.opentracing.span.HttpSpanTagger.tagging;

@API(status = EXPERIMENTAL)
public final class HttpContentLanguageSpanDecorator extends ForwardingSpanDecorator {

    private static final Tag<String> HTTP_CONTENT_LANGUAGE =
            new StringTag("http.content_language");

    public HttpContentLanguageSpanDecorator() {
        super(new HttpResponseHeaderSpanDecorator(
                tagging(HTTP_CONTENT_LANGUAGE, CONTENT_LANGUAGE)));
    }

}

package org.zalando.riptide.opentracing.span;

import io.opentracing.tag.StringTag;
import io.opentracing.tag.Tag;
import org.apiguardian.api.API;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;
import static org.zalando.riptide.opentracing.span.HttpSpanTagger.tagging;

/**
 * @see <a href="https://opensocial.github.io/spec/2.5.1/Core-API-Server.xml#rfc.section.2.1.1.1">OpenSocial Core API Server Specification 2.5.1, Section 2.1.1.1</a>
 */
@API(status = EXPERIMENTAL)
public final class HttpMethodOverrideSpanDecorator extends ForwardingSpanDecorator {

    private static final Tag<String> HTTP_METHOD_OVERRIDE =
            new StringTag("http.method_override");

    public HttpMethodOverrideSpanDecorator() {
        super(new HttpRequestHeaderSpanDecorator(tagging(HTTP_METHOD_OVERRIDE,
                "HTTP-Method-Override",
                "X-HTTP-Method-Override")));
    }

}

package org.zalando.riptide.opentracing.span;

import io.opentracing.*;
import org.zalando.riptide.*;
import org.zalando.riptide.opentracing.*;

import static java.util.Objects.*;

/**
 * Sets the <code>http.path</code> span tag, based on {@link RequestArguments#getUriTemplate()}.
 *
 * @see ExtensionTags#HTTP_PATH
 */
public final class HttpPathSpanDecorator implements SpanDecorator {

    @Override
    public void onRequest(final Span span, final RequestArguments arguments) {
        final String uriTemplate = arguments.getUriTemplate();

        if (nonNull(uriTemplate)) {
            span.setTag(ExtensionTags.HTTP_PATH, uriTemplate);
        }
    }

}

package org.zalando.riptide.opentracing.span;

import io.opentracing.Span;
import org.zalando.riptide.RequestArguments;
import org.zalando.riptide.opentracing.ExtensionTags;

import static java.util.Objects.nonNull;

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

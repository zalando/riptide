package org.zalando.riptide.opentracing.span;

import lombok.AllArgsConstructor;
import lombok.experimental.Delegate;

@AllArgsConstructor
abstract class ForwardingSpanDecorator implements SpanDecorator {

    @Delegate
    private final SpanDecorator delegate;

}

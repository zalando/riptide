package org.zalando.riptide.opentracing;

import com.google.common.collect.ForwardingObject;
import io.opentracing.Span;
import lombok.AllArgsConstructor;
import lombok.experimental.Delegate;

@AllArgsConstructor
abstract class ForwardingSpan extends ForwardingObject implements Span {

    @Delegate
    @Override
    protected abstract Span delegate();

}

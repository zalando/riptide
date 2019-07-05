package org.zalando.riptide.opentracing;

import io.opentracing.Span;
import io.opentracing.Tracer;
import org.zalando.riptide.RequestArguments;
import org.zalando.riptide.opentracing.span.SpanDecorator;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collection;

public interface LifecyclePolicy {

    @Nullable
    Span start(Tracer tracer, RequestArguments arguments, SpanDecorator decorator);

    void finish(Span span);

    static LifecyclePolicy composite(final LifecyclePolicy... lifecyclePolicies) {
        return composite(Arrays.asList(lifecyclePolicies));
    }

    static LifecyclePolicy composite(final Collection<LifecyclePolicy> lifecyclePolicies) {
        return new CompositeLifecyclePolicy(lifecyclePolicies);
    }

}

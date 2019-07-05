package org.zalando.riptide.opentracing;

import io.opentracing.Span;
import io.opentracing.Tracer;
import lombok.AllArgsConstructor;
import org.zalando.riptide.RequestArguments;
import org.zalando.riptide.opentracing.span.SpanDecorator;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.function.Consumer;

@AllArgsConstructor
final class CompositeLifecyclePolicy implements LifecyclePolicy {

    private final Collection<LifecyclePolicy> policies;

    @Nullable
    @Override
    public Span start(final Tracer tracer, final RequestArguments arguments, final SpanDecorator decorator) {
        for (final LifecyclePolicy policy : policies) {
            @Nullable final Span span = policy.start(tracer, arguments, decorator);

            if (span != null) {
                return new FinishingSpan(span, policy::finish);
            }
        }

        return null;
    }

    @Override
    public void finish(final Span span) {
        span.finish();
    }

    @AllArgsConstructor
    private static final class FinishingSpan extends ForwardingSpan {

        private final Span span;
        private final Consumer<Span> callback;

        @Override
        protected Span delegate() {
            return span;
        }

        @Override
        public void finish() {
            callback.accept(span);
        }

    }

}

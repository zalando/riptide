package org.zalando.riptide.opentracing;

import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import org.apiguardian.api.API;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;

@API(status = EXPERIMENTAL)
public final class NoOpActivation implements Activation {

    private enum NoOpScope implements Scope {
        INSTANCE;

        @Override
        public void close() {
            // nothing to do
        }
    }

    @Override
    public Scope activate(final Tracer tracer, final Span span) {
        return NoOpScope.INSTANCE;
    }

}

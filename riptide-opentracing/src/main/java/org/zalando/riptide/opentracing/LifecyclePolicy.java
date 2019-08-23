package org.zalando.riptide.opentracing;

import io.opentracing.Span;
import io.opentracing.Tracer;
import org.apiguardian.api.API;
import org.zalando.riptide.RequestArguments;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;

@API(status = EXPERIMENTAL)
public interface LifecyclePolicy {

    Optional<Span> start(Tracer tracer, RequestArguments arguments);

    void finish(Span span);

    static LifecyclePolicy composite(final LifecyclePolicy... lifecyclePolicies) {
        return composite(Arrays.asList(lifecyclePolicies));
    }

    static LifecyclePolicy composite(final Collection<LifecyclePolicy> lifecyclePolicies) {
        return new CompositeLifecyclePolicy(lifecyclePolicies);
    }

}

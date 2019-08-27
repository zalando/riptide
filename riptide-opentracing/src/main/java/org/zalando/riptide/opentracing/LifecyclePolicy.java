package org.zalando.riptide.opentracing;

import io.opentracing.*;
import org.zalando.riptide.*;

import java.util.*;

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

package org.zalando.riptide.opentracing;

import io.opentracing.*;
import lombok.*;
import org.zalando.riptide.*;

import java.util.*;

import static org.zalando.riptide.opentracing.OpenTracingPlugin.*;

@AllArgsConstructor
final class NewSpanLifecyclePolicy implements LifecyclePolicy {

    @Override
    public Optional<Span> start(final Tracer tracer, final RequestArguments arguments) {
        final String operationName = arguments.getAttribute(OPERATION_NAME)
                .orElse(arguments.getMethod().name());

        return Optional.of(tracer.buildSpan(operationName).start());
    }

    @Override
    public void finish(final Span span) {
        span.finish();
    }

}

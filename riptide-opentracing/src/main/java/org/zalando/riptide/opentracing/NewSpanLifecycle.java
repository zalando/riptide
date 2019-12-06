package org.zalando.riptide.opentracing;

import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.tag.Tags;
import lombok.AllArgsConstructor;
import org.apiguardian.api.API;
import org.zalando.riptide.RequestArguments;

import java.util.Optional;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;
import static org.zalando.riptide.opentracing.OpenTracingPlugin.OPERATION_NAME;

@API(status = EXPERIMENTAL)
@AllArgsConstructor
public final class NewSpanLifecycle implements Lifecycle {

    @Override
    public Optional<Span> start(final Tracer tracer, final RequestArguments arguments) {
        final String operationName = arguments.getAttribute(OPERATION_NAME)
                .orElse(arguments.getMethod().name());

        return Optional.of(tracer.buildSpan(operationName)
                .withTag(Tags.SPAN_KIND, Tags.SPAN_KIND_CLIENT)
                .start());
    }

}

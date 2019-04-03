package org.zalando.riptide.autoconfigure;

import io.opentracing.Tracer;
import org.zalando.riptide.Plugin;
import org.zalando.riptide.autoconfigure.RiptideProperties.Client;
import org.zalando.riptide.opentracing.OpenTracingPlugin;
import org.zalando.riptide.opentracing.span.RetrySpanDecorator;
import org.zalando.riptide.opentracing.span.SpanDecorator;
import org.zalando.riptide.opentracing.span.StaticSpanDecorator;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

import static org.zalando.riptide.opentracing.span.SpanDecorator.composite;

@SuppressWarnings("unused")
final class OpenTracingPluginFactory {

    private OpenTracingPluginFactory() {

    }

    public static Plugin create(final Tracer tracer, final Client client, @Nullable final SpanDecorator decorator) {
        final List<SpanDecorator> decorators = new ArrayList<>();
        decorators.add(new StaticSpanDecorator(client.getTracing().getTags()));

        if (client.getRetry().getEnabled()) {
            decorators.add(new RetrySpanDecorator());
        }

        return create(tracer, decorator)
                .withAdditionalSpanDecorators(composite(decorators));
    }

    private static OpenTracingPlugin create(final Tracer tracer,
            @Nullable final SpanDecorator decorator) {
        return decorator == null ?
                new OpenTracingPlugin(tracer) :
                new OpenTracingPlugin(tracer).withSpanDecorators(decorator);
    }

}

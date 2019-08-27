package org.zalando.riptide.autoconfigure;

import io.opentracing.*;
import org.zalando.riptide.*;
import org.zalando.riptide.autoconfigure.RiptideProperties.*;
import org.zalando.riptide.opentracing.*;
import org.zalando.riptide.opentracing.span.*;

import javax.annotation.*;
import java.util.*;

import static org.zalando.riptide.opentracing.span.SpanDecorator.*;

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

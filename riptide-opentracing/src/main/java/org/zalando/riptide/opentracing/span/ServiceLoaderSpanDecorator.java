package org.zalando.riptide.opentracing.span;

import org.apiguardian.api.API;

import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

import static java.util.ServiceLoader.load;
import static java.util.stream.StreamSupport.stream;
import static org.apiguardian.api.API.Status.EXPERIMENTAL;
import static org.zalando.riptide.opentracing.span.CompositeSpanDecorator.composite;

/**
 * @see ServiceLoader
 */
@API(status = EXPERIMENTAL)
public final class ServiceLoaderSpanDecorator extends ForwardingSpanDecorator {

    public ServiceLoaderSpanDecorator() {
        super(composite(loadDecorators()));
    }

    private static synchronized List<SpanDecorator> loadDecorators() {
        return stream(load(SpanDecorator.class).spliterator(), false).collect(Collectors.toList());
    }

}

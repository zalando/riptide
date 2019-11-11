package org.zalando.riptide.opentracing.span;

import org.apiguardian.api.API;

import java.util.ServiceLoader;

import static java.util.ServiceLoader.load;
import static org.apiguardian.api.API.Status.EXPERIMENTAL;
import static org.zalando.riptide.opentracing.span.CompositeSpanDecorator.composite;

/**
 * @see ServiceLoader
 */
@API(status = EXPERIMENTAL)
public final class ServiceLoaderSpanDecorator extends ForwardingSpanDecorator {

    public ServiceLoaderSpanDecorator() {
        super(composite(load(SpanDecorator.class)));
    }

}

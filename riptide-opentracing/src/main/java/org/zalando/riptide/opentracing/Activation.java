package org.zalando.riptide.opentracing;

import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import org.apiguardian.api.API;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;

@API(status = EXPERIMENTAL)
public interface Activation {

    Scope activate(Tracer tracer, Span span);

}

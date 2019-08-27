package org.zalando.riptide.opentracing;

import io.opentracing.*;
import org.apiguardian.api.*;

import static org.apiguardian.api.API.Status.*;

@API(status = EXPERIMENTAL)
public interface ActivationPolicy {

    Runnable activate(Tracer tracer, Span span);

}

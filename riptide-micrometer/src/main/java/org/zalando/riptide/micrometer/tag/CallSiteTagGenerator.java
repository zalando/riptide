package org.zalando.riptide.micrometer.tag;

import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import org.apiguardian.api.API;
import org.zalando.riptide.RequestArguments;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;
import static org.zalando.riptide.micrometer.MicrometerPlugin.TAGS;

/**
 * <strong>Beware</strong> that some meter registries, e.g. Prometheus, only
 * allow a fixed set of tag keys for the same metric. That means if tags are
 * being passed from a call site they should be passed for all calls. Either
 * by adjusting all call sites or by defining a custom tag generator that
 * provides default values.
 */
@API(status = EXPERIMENTAL)
public final class CallSiteTagGenerator implements TagGenerator {

    @Override
    public Iterable<Tag> onRequest(final RequestArguments arguments) {
        return arguments.getAttribute(TAGS).orElse(Tags.empty());
    }

}

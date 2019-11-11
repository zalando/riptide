package org.zalando.riptide.micrometer.tag;

import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import org.apiguardian.api.API;
import org.zalando.riptide.RequestArguments;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;
import static org.zalando.riptide.micrometer.MicrometerPlugin.TAGS;

@API(status = EXPERIMENTAL)
public final class CallSiteTagGenerator implements TagGenerator {

    @Override
    public Iterable<Tag> onRequest(final RequestArguments arguments) {
        return arguments.getAttribute(TAGS).orElse(Tags.empty());
    }

}

package org.zalando.riptide.micrometer.tag;

import io.micrometer.core.instrument.Tag;
import org.zalando.riptide.Attributes;
import org.zalando.riptide.RequestArguments;

import static java.util.Collections.singleton;
import static org.zalando.riptide.Attributes.RETRIES;

/**
 * @see Attributes#RETRIES
 */
public final class RetryTagGenerator implements TagGenerator {

    @Override
    public Iterable<Tag> onRequest(final RequestArguments arguments) {
        final int retries = arguments.getAttribute(RETRIES).orElse(0);
        return singleton(Tag.of("retry_number", String.valueOf(retries)));
    }

}

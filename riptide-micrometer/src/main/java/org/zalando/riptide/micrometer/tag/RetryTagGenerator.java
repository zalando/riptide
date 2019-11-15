package org.zalando.riptide.micrometer.tag;

import io.micrometer.core.instrument.Tag;
import org.zalando.riptide.Attributes;
import org.zalando.riptide.RequestArguments;
import org.zalando.riptide.micrometer.tag.TagGenerator;

import java.util.Collections;

import static java.util.Collections.emptySet;

/**
 * @see Attributes#RETRIES
 */
public final class RetryTagGenerator implements TagGenerator {

    @Override
    public Iterable<Tag> onRequest(final RequestArguments arguments) {
        return arguments.getAttribute(Attributes.RETRIES)
                .map(String::valueOf)
                .map(attempts -> Tag.of("retry_number", attempts))
                .map(Collections::singleton)
                .orElse(emptySet());
    }

}

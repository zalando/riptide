package org.zalando.riptide.micrometer.tag;

import com.google.common.collect.Lists;
import io.micrometer.core.instrument.Tag;
import org.apiguardian.api.API;
import org.springframework.http.client.ClientHttpResponse;
import org.zalando.riptide.RequestArguments;

import javax.annotation.CheckReturnValue;
import java.io.IOException;
import java.util.Collections;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;

@API(status = EXPERIMENTAL)
public interface TagGenerator {

    @CheckReturnValue
    default Iterable<Tag> onRequest(final RequestArguments arguments) {
        return Collections.emptyList();
    }

    @CheckReturnValue
    default Iterable<Tag> onResponse(
            final RequestArguments arguments,
            final ClientHttpResponse response) throws IOException {
        return Collections.emptyList();
    }

    @CheckReturnValue
    default Iterable<Tag> onError(
            final RequestArguments arguments,
            final Throwable throwable) {
        return Collections.emptyList();
    }

    static TagGenerator composite(final TagGenerator generator, final TagGenerator... generators) {
        return composite(Lists.asList(generator, generators));
    }

    static TagGenerator composite(final Iterable<TagGenerator> generators) {
        return new CompositeTagGenerator(generators);
    }

}

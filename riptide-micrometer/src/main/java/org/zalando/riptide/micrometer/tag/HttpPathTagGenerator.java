package org.zalando.riptide.micrometer.tag;

import io.micrometer.core.instrument.Tag;
import org.apiguardian.api.API;
import org.zalando.riptide.RequestArguments;

import javax.annotation.Nullable;
import java.util.Set;

import static java.util.Collections.singleton;
import static org.apiguardian.api.API.Status.EXPERIMENTAL;

@API(status = EXPERIMENTAL)
public final class HttpPathTagGenerator implements TagGenerator {

    private static final String HTTP_PATH = "http.path";
    private static final Set<Tag> NONE = singleton(Tag.of(HTTP_PATH, ""));

    @Override
    public Iterable<Tag> onRequest(final RequestArguments arguments) {
        @Nullable final String uriTemplate = arguments.getUriTemplate();

        if (uriTemplate == null) {
            return NONE;
        }

        return singleton(Tag.of(HTTP_PATH, uriTemplate));
    }

}

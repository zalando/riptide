package org.zalando.riptide.micrometer.tag;

import com.google.common.base.Throwables;
import io.micrometer.core.instrument.Tag;
import lombok.AllArgsConstructor;
import org.apiguardian.api.API;
import org.springframework.http.client.ClientHttpResponse;
import org.zalando.riptide.RequestArguments;

import java.util.Set;
import java.util.function.UnaryOperator;

import static java.util.Collections.singleton;
import static org.apiguardian.api.API.Status.EXPERIMENTAL;

@API(status = EXPERIMENTAL)
@AllArgsConstructor
public final class ErrorKindTagGenerator implements TagGenerator {

    private static final String ERROR_KIND = "error.kind";
    private static final Set<Tag> NONE = singleton(Tag.of(ERROR_KIND, "none"));

    private final UnaryOperator<Throwable> extractor;

    public ErrorKindTagGenerator() {
        this(Throwables::getRootCause);
    }

    @Override
    public Iterable<Tag> onResponse(
            final RequestArguments arguments,
            final ClientHttpResponse response) {

        return NONE;
    }

    @Override
    public Iterable<Tag> onError(
            final RequestArguments arguments,
            final Throwable throwable) {

        final Throwable error = extractor.apply(throwable);
        final String name = error.getClass().getSimpleName();
        return singleton(Tag.of(ERROR_KIND, name));
    }

}

package org.zalando.riptide.metrics;

import io.micrometer.core.instrument.Tag;
import org.apiguardian.api.API;
import org.springframework.http.client.ClientHttpResponse;
import org.zalando.riptide.RequestArguments;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Arrays;

import static com.google.common.base.MoreObjects.firstNonNull;
import static org.apiguardian.api.API.Status.EXPERIMENTAL;

@API(status = EXPERIMENTAL)
public final class DefaultTagGenerator implements TagGenerator {

    @Override
    public Iterable<Tag> tags(final RequestArguments arguments, @Nullable final ClientHttpResponse response,
            @Nullable final Throwable throwable) {
        return Arrays.asList(
                Tag.of("method", method(arguments)),
                Tag.of("uri", uri(arguments)),
                Tag.of("status", status(response)),
                Tag.of("clientName", client(arguments)),
                Tag.of("exception", exception(throwable))
        );
    }

    private String method(final RequestArguments arguments) {
        return arguments.getMethod().name();
    }

    private String uri(final RequestArguments arguments) {
        return firstNonNull(arguments.getUriTemplate(), arguments.getRequestUri().getPath());
    }

    private String status(@Nullable final ClientHttpResponse response) {
        if (response == null) {
            return "CLIENT_ERROR";
        }

        try {
            return String.valueOf(response.getRawStatusCode());
        } catch (final IOException e) {
            return "IO_ERROR";
        }
    }

    private String client(final RequestArguments arguments) {
        return firstNonNull(arguments.getRequestUri().getHost(), "none");
    }

    private String exception(@Nullable final Throwable throwable) {
        return throwable == null ? "None" : throwable.getClass().getSimpleName();
    }

}

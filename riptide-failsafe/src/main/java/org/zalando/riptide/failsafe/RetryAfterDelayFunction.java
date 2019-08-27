package org.zalando.riptide.failsafe;

import lombok.extern.slf4j.*;
import net.jodah.failsafe.*;
import net.jodah.failsafe.function.*;
import org.apiguardian.api.*;
import org.springframework.http.client.*;
import org.zalando.riptide.*;

import java.time.*;
import java.util.*;

import static org.apiguardian.api.API.Status.*;

/**
 * @see <a href="https://tools.ietf.org/html/rfc7231#section-7.1.3">RFC 7231, section 7.1.3: Retry-After</a>
 */
@API(status = EXPERIMENTAL)
@Slf4j
public final class RetryAfterDelayFunction implements DelayFunction<ClientHttpResponse, Throwable> {

    private final DelayParser parser;

    public RetryAfterDelayFunction(final Clock clock) {
        this.parser = new CompositeDelayParser(Arrays.asList(
                new SecondsDelayParser(),
                new HttpDateDelayParser(clock)
        ));
    }

    @Override
    public Duration computeDelay(final ClientHttpResponse result, final Throwable failure, final ExecutionContext context) {
        return Optional.ofNullable(failure)
                .filter(HttpResponseException.class::isInstance)
                .map(HttpResponseException.class::cast)
                .map(response -> response.getResponseHeaders().getFirst("Retry-After"))
                .map(parser::parse)
                .orElse(null);
    }

}

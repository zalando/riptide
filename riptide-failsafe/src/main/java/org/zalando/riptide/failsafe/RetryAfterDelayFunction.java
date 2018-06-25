package org.zalando.riptide.failsafe;

import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.ExecutionContext;
import net.jodah.failsafe.RetryPolicy.DelayFunction;
import net.jodah.failsafe.util.Duration;
import org.apiguardian.api.API;
import org.zalando.riptide.HttpResponseException;

import java.time.Clock;
import java.util.Arrays;
import java.util.Optional;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;

/**
 * @see <a href="https://tools.ietf.org/html/rfc7231#section-7.1.3">RFC 7231, section 7.1.3: Retry-After</a>
 */
@API(status = EXPERIMENTAL)
@Slf4j
public final class RetryAfterDelayFunction implements DelayFunction<Object, Throwable> {

    private final DelayParser parser;

    public RetryAfterDelayFunction(final Clock clock) {
        this.parser = new CompoundDelayParser(Arrays.asList(
                new SecondsDelayParser(),
                new HttpDateDelayParser(clock)
        ));
    }

    @Override
    public Duration computeDelay(final Object result, final Throwable failure, final ExecutionContext context) {
        return Optional.ofNullable(failure)
                .filter(HttpResponseException.class::isInstance)
                .map(HttpResponseException.class::cast)
                .map(response -> response.getResponseHeaders().getFirst("Retry-After"))
                .map(parser::parse)
                .orElse(null);
    }

}

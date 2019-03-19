package org.zalando.riptide.failsafe;

import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.ExecutionContext;
import net.jodah.failsafe.RetryPolicy.DelayFunction;
import org.apiguardian.api.API;
import org.springframework.http.client.ClientHttpResponse;
import org.zalando.riptide.HttpResponseException;

import java.time.Clock;
import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;

/**
 * @see <a href="https://opensource.zalando.com/restful-api-guidelines/#153">Zalando RESTful API Guidelines, Rule #153 Use Code 429 with Headers for Rate Limits</a>
 */
@API(status = EXPERIMENTAL)
@Slf4j
public final class RateLimitResetDelayFunction implements DelayFunction<ClientHttpResponse, Throwable> {

    private final DelayParser parser;

    public RateLimitResetDelayFunction(final Clock clock) {
        this.parser = new CompositeDelayParser(Arrays.asList(
                new EpochSecondsDelayParser(clock),
                new SecondsDelayParser()
        ));
    }

    @Override
    public Duration computeDelay(final ClientHttpResponse result, final Throwable failure, final ExecutionContext context) {
        return Optional.ofNullable(failure)
                .filter(HttpResponseException.class::isInstance)
                .map(HttpResponseException.class::cast)
                .map(response -> response.getResponseHeaders().getFirst("X-RateLimit-Reset"))
                .map(parser::parse)
                .orElse(null);
    }

}

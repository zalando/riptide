package org.zalando.riptide.failsafe;

import dev.failsafe.function.ContextualSupplier;
import lombok.extern.slf4j.Slf4j;
import dev.failsafe.ExecutionContext;
import org.apiguardian.api.API;
import org.springframework.http.client.ClientHttpResponse;
import org.zalando.riptide.HttpResponseException;

import java.time.Clock;
import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

import static java.util.Optional.ofNullable;
import static org.apiguardian.api.API.Status.EXPERIMENTAL;

/**
 * @see <a href="https://opensource.zalando.com/restful-api-guidelines/#153">Zalando RESTful API Guidelines, Rule #153 Use Code 429 with Headers for Rate Limits</a>
 */
@API(status = EXPERIMENTAL)
@Slf4j
public final class RateLimitResetDelayFunction implements ContextualSupplier<ClientHttpResponse, Duration> {

    private final DelayParser parser;

    public RateLimitResetDelayFunction(final Clock clock) {
        this.parser = new CompositeDelayParser(Arrays.asList(
                new EpochSecondsDelayParser(clock),
                new SecondsDelayParser()
        ));
    }

    @Override
    public Duration get(final ExecutionContext<ClientHttpResponse> context) {
        return ofNullable(context)
                .map(ExecutionContext::getLastException)
                .filter(HttpResponseException.class::isInstance)
                .map(HttpResponseException.class::cast)
                .map(response -> response.getResponseHeaders().getFirst("X-RateLimit-Reset"))
                .map(parser::parse)
                .orElse(Duration.ofMinutes(-1));
    }

}

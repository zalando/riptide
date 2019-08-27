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

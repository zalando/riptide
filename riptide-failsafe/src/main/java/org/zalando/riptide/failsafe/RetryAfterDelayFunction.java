package org.zalando.riptide.failsafe;

import net.jodah.failsafe.ExecutionContext;
import net.jodah.failsafe.RetryPolicy.DelayFunction;
import net.jodah.failsafe.util.Duration;
import org.zalando.riptide.HttpResponseException;

import javax.annotation.Nullable;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static java.lang.Long.parseLong;
import static java.time.Duration.between;
import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;

/**
 * @see <a href="https://tools.ietf.org/html/rfc7231#section-7.1.3">RFC 7231, section 7.1.3: Retry-After</a>
 */
public final class RetryAfterDelayFunction implements DelayFunction<Object, HttpResponseException> {

    private final Pattern digit = Pattern.compile("\\d");

    private final Clock clock;

    public RetryAfterDelayFunction(final Clock clock) {
        this.clock = clock;
    }

    @Override
    public Duration computeDelay(final Object result, final HttpResponseException failure,
            final ExecutionContext context) {

        /*
         * The value of this field can be either an HTTP-date or a number of seconds to delay after the response
         * is received.
         *
         * Retry-After = HTTP-date / delay-seconds
         */
        @Nullable final String retryAfter = failure.getResponseHeaders().getFirst("Retry-After");

        if (retryAfter == null) {
            return null;
        } else if (onlyDigits(retryAfter)) {
            return parseDelaySeconds(retryAfter);
        } else {
            return parseHttpDate(retryAfter);
        }
    }

    private boolean onlyDigits(final String s) {
        return digit.matcher(s).matches();
    }

    private Duration parseDelaySeconds(final String retryAfter) {
        return new Duration(parseLong(retryAfter), TimeUnit.SECONDS);
    }

    private Duration parseHttpDate(final String retryAfter) {
        final OffsetDateTime dateTime = OffsetDateTime.parse(retryAfter, RFC_1123_DATE_TIME);
        final long seconds = between(OffsetDateTime.now(clock), dateTime).getSeconds();
        return new Duration(seconds, TimeUnit.SECONDS);
    }

}

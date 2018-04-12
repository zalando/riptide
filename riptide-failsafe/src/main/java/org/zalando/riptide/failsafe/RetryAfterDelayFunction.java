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
import static java.time.OffsetDateTime.now;
import static java.time.OffsetDateTime.parse;
import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;

/**
 * @see <a href="https://tools.ietf.org/html/rfc7231#section-7.1.3">RFC 7231, section 7.1.3: Retry-After</a>
 */
public final class RetryAfterDelayFunction implements DelayFunction<Object, Throwable> {

    private final Pattern digit = Pattern.compile("\\d");

    private final Clock clock;

    public RetryAfterDelayFunction(final Clock clock) {
        this.clock = clock;
    }

    @Override
    public Duration computeDelay(final Object result, final Throwable failure, final ExecutionContext context) {
        return failure instanceof HttpResponseException ? computeDelay((HttpResponseException) failure) : null;
    }

    @Nullable
    private Duration computeDelay(final HttpResponseException failure) {
        @Nullable final String retryAfter = failure.getResponseHeaders().getFirst("Retry-After");
        return retryAfter == null ? null : toDuration(parseDelay(retryAfter));
    }

    /**
     * The value of this field can be either an HTTP-date or a number of seconds to delay after the response
     * is received.
     *
     * Retry-After = HTTP-date / delay-seconds
     *
     * @param retryAfter non-null header value
     * @return the parsed delay in seconds
     */
    private long parseDelay(final String retryAfter) {
        return onlyDigits(retryAfter) ?
                parseSeconds(retryAfter) :
                secondsUntil(parseDate(retryAfter));
    }

    private boolean onlyDigits(final String s) {
        return digit.matcher(s).matches();
    }

    private long parseSeconds(final String retryAfter) {
        return parseLong(retryAfter);
    }

    private OffsetDateTime parseDate(final String retryAfter) {
        return parse(retryAfter, RFC_1123_DATE_TIME);
    }

    private long secondsUntil(final OffsetDateTime end) {
        return between(now(clock), end).getSeconds();
    }

    private Duration toDuration(final long seconds) {
        return new Duration(seconds, TimeUnit.SECONDS);
    }

}

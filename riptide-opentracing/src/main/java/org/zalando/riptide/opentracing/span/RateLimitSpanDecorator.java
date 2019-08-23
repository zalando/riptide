package org.zalando.riptide.opentracing.span;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.apiguardian.api.API;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;
import static org.zalando.riptide.opentracing.span.HttpSpanLogger.logging;

/**
 * @see <a href="https://ioggstream.github.io/draft-polli-ratelimit-headers/draft-polli-ratelimit-headers.html">Rate-Limit Header Fields for HTTP</a>
 * @see <a href="https://godoc.org/github.com/zalando/skipper/ratelimit#Lookuper">Skipper Rate Limiting</a>
 * @see <a href="https://developer.github.com/v3/rate_limit/">Github REST API v3: Rate Limit</a>
 */
@API(status = EXPERIMENTAL)
public final class RateLimitSpanDecorator extends ForwardingSpanDecorator {

    private static final String LIMIT = "rate_limit.limit";
    private static final String REMAINING = "rate_limit.remaining";
    private static final String RESET = "rate_limit.reset";

    public RateLimitSpanDecorator() {
        super(new HttpResponseHeaderSpanDecorator(logging(ImmutableMap.of(
                LIMIT,
                ImmutableList.of(
                        "Rate-Limit",
                        "X-Rate-Limit",
                        "RateLimit-Limit",
                        "X-RateLimit-Limit"),
                REMAINING,
                ImmutableList.of(
                        "RateLimit-Remaining",
                        "X-RateLimit-Remaining"),
                RESET,
                ImmutableList.of(
                        "RateLimit-Reset",
                        "X-RateLimit-Reset")
        ))));
    }

}

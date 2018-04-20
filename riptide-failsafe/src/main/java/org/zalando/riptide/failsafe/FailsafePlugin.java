package org.zalando.riptide.failsafe;

import net.jodah.failsafe.CircuitBreaker;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.apiguardian.api.API;
import org.springframework.http.client.ClientHttpResponse;
import org.zalando.riptide.Plugin;
import org.zalando.riptide.RequestArguments;
import org.zalando.riptide.RequestExecution;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;

import static org.apiguardian.api.API.Status.STABLE;
import static org.zalando.riptide.CancelableCompletableFuture.forwardTo;
import static org.zalando.riptide.CancelableCompletableFuture.preserveCancelability;

@API(status = STABLE)
public final class FailsafePlugin implements Plugin {

    private static final RetryPolicy NEVER = new RetryPolicy().withMaxRetries(0);

    private final ScheduledExecutorService scheduler;
    private final RetryPolicy retryPolicy;
    private final CircuitBreaker circuitBreaker;

    public FailsafePlugin(final ScheduledExecutorService scheduler) {
        this(scheduler, NEVER, new CircuitBreaker());
    }

    // used by spring-boot-starter
    FailsafePlugin(final ScheduledExecutorService scheduler, final RetryPolicy retryPolicy,
            final CircuitBreaker circuitBreaker) {
        this.scheduler = scheduler;
        this.retryPolicy = retryPolicy;
        this.circuitBreaker = circuitBreaker;
    }

    public FailsafePlugin withRetryPolicy(final RetryPolicy retryPolicy) {
        return new FailsafePlugin(scheduler, retryPolicy, circuitBreaker);
    }

    public FailsafePlugin withCircuitBreaker(final CircuitBreaker circuitBreaker) {
        return new FailsafePlugin(scheduler, retryPolicy, circuitBreaker);
    }

    @Override
    public RequestExecution prepare(final RequestArguments arguments, final RequestExecution execution) {
        return () -> {
            final CompletableFuture<ClientHttpResponse> original = Failsafe
                    .with(retryPolicy)
                    .with(circuitBreaker)
                    .with(scheduler)
                    // TODO allow to register listeners
                    .future(execution::execute);

            final CompletableFuture<ClientHttpResponse> cancelable = preserveCancelability(original);
            original.whenComplete(forwardTo(cancelable));
            return cancelable;
        };
    }

}

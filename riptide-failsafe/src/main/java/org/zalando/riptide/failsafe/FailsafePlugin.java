package org.zalando.riptide.failsafe;

import net.jodah.failsafe.CircuitBreaker;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.Listeners;
import net.jodah.failsafe.RetryPolicy;
import org.zalando.riptide.Plugin;
import org.zalando.riptide.RequestArguments;
import org.zalando.riptide.RequestExecution;

import java.util.concurrent.ScheduledExecutorService;

public final class FailsafePlugin implements Plugin {

    private static final RetryPolicy NEVER = new RetryPolicy().withMaxRetries(0);

    private final ScheduledExecutorService scheduler;
    private final RetryPolicy retryPolicy;
    private final CircuitBreaker circuitBreaker;

    public FailsafePlugin(final ScheduledExecutorService scheduler) {
        this(scheduler, NEVER, new CircuitBreaker());
    }

    private FailsafePlugin(final ScheduledExecutorService scheduler, final RetryPolicy retryPolicy,
            final CircuitBreaker circuitBreaker) {
        this.scheduler = scheduler;
        this.retryPolicy = retryPolicy;
        this.circuitBreaker = circuitBreaker;
    }

    public FailsafePlugin withRetryPolicy(final RetryPolicy retryPolicy) {
        return new FailsafePlugin(scheduler, new RetryPolicy(retryPolicy)
                // TODO temporary exception
                .retryOn(RetryException.class),
                circuitBreaker);
    }

    public FailsafePlugin withCircuitBreaker(final CircuitBreaker circuitBreaker) {
        return new FailsafePlugin(scheduler, retryPolicy, circuitBreaker);
    }

    @Override
    public RequestExecution prepare(final RequestArguments arguments, final RequestExecution execution) {
        return () -> Failsafe
                .with(retryPolicy)
                .with(circuitBreaker)
                .with(scheduler)
                .future(execution::execute);
    }

}

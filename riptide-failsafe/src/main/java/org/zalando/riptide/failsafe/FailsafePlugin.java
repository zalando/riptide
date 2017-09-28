package org.zalando.riptide.failsafe;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.Wither;
import net.jodah.failsafe.CircuitBreaker;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.zalando.riptide.Plugin;
import org.zalando.riptide.RequestArguments;
import org.zalando.riptide.RequestExecution;

import java.util.concurrent.ScheduledExecutorService;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class FailsafePlugin implements Plugin {

    private final ScheduledExecutorService scheduler;

    @Wither
    private final RetryPolicy retryPolicy;

    @Wither
    private final CircuitBreaker circuitBreaker;

    public FailsafePlugin(final ScheduledExecutorService scheduler) {
        this(scheduler, new RetryPolicy(), new CircuitBreaker());
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

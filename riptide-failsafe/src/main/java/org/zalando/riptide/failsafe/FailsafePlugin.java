package org.zalando.riptide.failsafe;

import net.jodah.failsafe.CircuitBreaker;
import net.jodah.failsafe.ExecutionContext;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.Listeners;
import net.jodah.failsafe.RetryPolicy;
import org.apiguardian.api.API;
import org.springframework.http.client.ClientHttpResponse;
import org.zalando.riptide.Plugin;
import org.zalando.riptide.RequestArguments;
import org.zalando.riptide.RequestExecution;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;

import static org.apiguardian.api.API.Status.INTERNAL;
import static org.apiguardian.api.API.Status.STABLE;
import static org.zalando.riptide.CancelableCompletableFuture.forwardTo;
import static org.zalando.riptide.CancelableCompletableFuture.preserveCancelability;

@API(status = STABLE)
public final class FailsafePlugin implements Plugin {

    private static final RetryPolicy NEVER = new RetryPolicy().withMaxRetries(0);

    private final ScheduledExecutorService scheduler;
    private final RetryPolicy retryPolicy;
    private final CircuitBreaker circuitBreaker;
    private final RetryListener listener;

    public FailsafePlugin(final ScheduledExecutorService scheduler) {
        this(scheduler, NEVER, new CircuitBreaker(), RetryListener.DEFAULT);
    }

    // used by riptide-spring-boot-starter
    @API(status = INTERNAL)
    FailsafePlugin(final ScheduledExecutorService scheduler, final RetryPolicy retryPolicy,
            final CircuitBreaker circuitBreaker, final RetryListener listener) {
        this.scheduler = scheduler;
        this.retryPolicy = retryPolicy;
        this.circuitBreaker = circuitBreaker;
        this.listener = listener;
    }

    public FailsafePlugin withRetryPolicy(final RetryPolicy retryPolicy) {
        return new FailsafePlugin(scheduler, retryPolicy, circuitBreaker, listener);
    }

    public FailsafePlugin withCircuitBreaker(final CircuitBreaker circuitBreaker) {
        return new FailsafePlugin(scheduler, retryPolicy, circuitBreaker, listener);
    }

    public FailsafePlugin withListener(final RetryListener listener) {
        return new FailsafePlugin(scheduler, retryPolicy, circuitBreaker, listener);
    }

    @Override
    public RequestExecution prepare(final RequestArguments arguments, final RequestExecution execution) {
        return () -> {
            final CompletableFuture<ClientHttpResponse> original = Failsafe
                    .with(retryPolicy)
                    .with(circuitBreaker)
                    .with(scheduler)
                    .with(new RetryListenersAdapter(listener, arguments))
                    .future(execution::execute);

            final CompletableFuture<ClientHttpResponse> cancelable = preserveCancelability(original);
            original.whenComplete(forwardTo(cancelable));
            return cancelable;
        };
    }

    private static final class RetryListenersAdapter extends Listeners<ClientHttpResponse> {

        private final RequestArguments arguments;
        private RetryListener listeners;

        public RetryListenersAdapter(final RetryListener listeners, final RequestArguments arguments) {
            this.arguments = arguments;
            this.listeners = listeners;
        }

        @Override
        public void onRetry(final ClientHttpResponse result, final Throwable failure,
                final ExecutionContext context) {
            listeners.onRetry(arguments, result, failure, context);
        }

    }

}

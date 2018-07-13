package org.zalando.riptide.failsafe;

import com.google.common.annotations.VisibleForTesting;
import net.jodah.failsafe.CircuitBreaker;
import net.jodah.failsafe.ExecutionContext;
import net.jodah.failsafe.Listeners;
import net.jodah.failsafe.RetryPolicy;
import net.jodah.failsafe.SyncFailsafe;
import org.apiguardian.api.API;
import org.springframework.http.client.ClientHttpResponse;
import org.zalando.riptide.Plugin;
import org.zalando.riptide.RequestArguments;
import org.zalando.riptide.RequestExecution;

import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;

import static net.jodah.failsafe.Failsafe.with;
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
        this(scheduler, NEVER, null, RetryListener.DEFAULT);
    }

    private FailsafePlugin(final ScheduledExecutorService scheduler, final RetryPolicy retryPolicy,
            @Nullable final CircuitBreaker circuitBreaker, final RetryListener listener) {
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
            final CompletableFuture<ClientHttpResponse> original =
                    failsafe()
                    .with(scheduler)
                    .with(new RetryListenersAdapter(listener, arguments))
                    .future(execution::execute);

            final CompletableFuture<ClientHttpResponse> cancelable = preserveCancelability(original);
            original.whenComplete(forwardTo(cancelable));
            return cancelable;
        };
    }

    SyncFailsafe<Object> failsafe() {
        final SyncFailsafe<Object> failsafe = with(retryPolicy);
        return circuitBreaker == null ? failsafe : failsafe.with(circuitBreaker);
    }

    @VisibleForTesting
    static final class RetryListenersAdapter extends Listeners<ClientHttpResponse> {

        private final RequestArguments arguments;
        private RetryListener listener;

        public RetryListenersAdapter(final RetryListener listener, final RequestArguments arguments) {
            this.arguments = arguments;
            this.listener = listener;
        }

        @Override
        public void onRetry(final ClientHttpResponse result, final Throwable failure,
                final ExecutionContext context) {
            listener.onRetry(arguments, result, failure, context);
        }

    }

}

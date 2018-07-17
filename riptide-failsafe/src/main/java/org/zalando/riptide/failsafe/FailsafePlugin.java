package org.zalando.riptide.failsafe;

import com.google.common.annotations.VisibleForTesting;
import lombok.AllArgsConstructor;
import net.jodah.failsafe.CircuitBreaker;
import net.jodah.failsafe.ExecutionContext;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.Listeners;
import net.jodah.failsafe.RetryPolicy;
import net.jodah.failsafe.SyncFailsafe;
import org.apiguardian.api.API;
import org.springframework.http.client.ClientHttpResponse;
import org.zalando.riptide.ConditionalIdempotentMethodDetector;
import org.zalando.riptide.DefaultIdempotentMethodDetector;
import org.zalando.riptide.DefaultSafeMethodDetector;
import org.zalando.riptide.IdempotencyKeyIdempotentMethodDetector;
import org.zalando.riptide.MethodDetector;
import org.zalando.riptide.OverrideSafeMethodDetector;
import org.zalando.riptide.Plugin;
import org.zalando.riptide.RequestArguments;
import org.zalando.riptide.RequestExecution;

import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;

import static lombok.AccessLevel.PRIVATE;
import static org.apiguardian.api.API.Status.STABLE;
import static org.zalando.riptide.CancelableCompletableFuture.forwardTo;
import static org.zalando.riptide.CancelableCompletableFuture.preserveCancelability;

@API(status = STABLE)
@AllArgsConstructor(access = PRIVATE)
public final class FailsafePlugin implements Plugin {

    private final ScheduledExecutorService scheduler;
    private final MethodDetector idempotent;
    private final RetryPolicy retryPolicy;
    private final CircuitBreaker circuitBreaker;
    private final RetryListener listener;

    public FailsafePlugin(final ScheduledExecutorService scheduler) {
        this(scheduler, MethodDetector.compound(
                new DefaultIdempotentMethodDetector(MethodDetector.compound(
                        new DefaultSafeMethodDetector(),
                        new OverrideSafeMethodDetector()
                )),
                new ConditionalIdempotentMethodDetector(),
                new IdempotencyKeyIdempotentMethodDetector()
        ), null, null, RetryListener.DEFAULT);
    }

    public FailsafePlugin withIdempotentMethodDetector(final MethodDetector detector) {
        return new FailsafePlugin(scheduler, detector, retryPolicy, circuitBreaker, listener);
    }

    public FailsafePlugin withRetryPolicy(@Nullable final RetryPolicy retryPolicy) {
        return new FailsafePlugin(scheduler, idempotent, retryPolicy, circuitBreaker, listener);
    }

    public FailsafePlugin withCircuitBreaker(@Nullable final CircuitBreaker circuitBreaker) {
        return new FailsafePlugin(scheduler, idempotent, retryPolicy, circuitBreaker, listener);
    }

    public FailsafePlugin withListener(final RetryListener listener) {
        return new FailsafePlugin(scheduler, idempotent, retryPolicy, circuitBreaker, listener);
    }

    @Override
    public RequestExecution prepare(final RequestArguments arguments, final RequestExecution execution) {
        @Nullable final SyncFailsafe<Object> failsafe = select(retryPolicy, circuitBreaker, arguments);

        if (failsafe == null) {
            // TODO https://github.com/zalando/riptide/issues/442
            return execution;
        }

        return () -> {
            final CompletableFuture<ClientHttpResponse> original = failsafe
                    .with(scheduler)
                    .with(new RetryListenersAdapter(listener, arguments))
                    .future(execution::execute);

            final CompletableFuture<ClientHttpResponse> cancelable = preserveCancelability(original);
            original.whenComplete(forwardTo(cancelable));
            return cancelable;
        };
    }

    @Nullable
    private SyncFailsafe<Object> select(@Nullable final RetryPolicy retryPolicy,
            @Nullable final CircuitBreaker circuitBreaker, final RequestArguments arguments) {

        if (retryPolicy != null && !idempotent.test(arguments)) {
            return select(null, circuitBreaker, arguments);
        }

        if (retryPolicy == null && circuitBreaker == null) {
            return null;
        } else if (retryPolicy == null) {
            return Failsafe.with(circuitBreaker);
        } else if (circuitBreaker == null) {
            return Failsafe.with(retryPolicy);
        } else {
            return Failsafe.with(retryPolicy).with(circuitBreaker);
        }
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

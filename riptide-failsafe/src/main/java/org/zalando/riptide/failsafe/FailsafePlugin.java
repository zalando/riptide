package org.zalando.riptide.failsafe;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import lombok.AllArgsConstructor;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.Policy;
import net.jodah.failsafe.RetryPolicy;
import net.jodah.failsafe.event.ExecutionAttemptedEvent;
import net.jodah.failsafe.function.CheckedConsumer;
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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import static lombok.AccessLevel.PRIVATE;
import static org.apiguardian.api.API.Status.MAINTAINED;
import static org.zalando.riptide.CancelableCompletableFuture.forwardTo;
import static org.zalando.riptide.CancelableCompletableFuture.preserveCancelability;

@API(status = MAINTAINED)
@AllArgsConstructor(access = PRIVATE)
public final class FailsafePlugin implements Plugin {

    private final ImmutableList<? extends Policy<ClientHttpResponse>> policies;
    private final ScheduledExecutorService scheduler;
    private final MethodDetector idempotent;
    private final RetryListener listener;

    public FailsafePlugin(final ImmutableList<? extends Policy<ClientHttpResponse>> policies,
            final ScheduledExecutorService scheduler) {
        this(policies, scheduler, MethodDetector.compound(
                new DefaultIdempotentMethodDetector(MethodDetector.compound(
                        new DefaultSafeMethodDetector(),
                        new OverrideSafeMethodDetector()
                )),
                new ConditionalIdempotentMethodDetector(),
                new IdempotencyKeyIdempotentMethodDetector()
        ), RetryListener.DEFAULT);
    }

    public FailsafePlugin withIdempotentMethodDetector(final MethodDetector idempotent) {
        return new FailsafePlugin(policies, scheduler, idempotent, listener);
    }

    public FailsafePlugin withListener(final RetryListener listener) {
        return new FailsafePlugin(policies, scheduler, idempotent, listener);
    }

    @Override
    public RequestExecution beforeDispatch(final RequestExecution execution) {
        return arguments -> {
            final Policy<ClientHttpResponse>[] policies = select(arguments);

            if (policies.length == 0) {
                return execution.execute(arguments);
            }

            final CompletableFuture<ClientHttpResponse> original = Failsafe.with(select(arguments))
                    .with(scheduler)
                    .getStageAsync(() -> execution.execute(arguments));

            final CompletableFuture<ClientHttpResponse> cancelable = preserveCancelability(original);
            original.whenComplete(forwardTo(cancelable));
            return cancelable;
        };
    }


    private Policy<ClientHttpResponse>[] select(final RequestArguments arguments) {
        final Stream<Policy<ClientHttpResponse>> stream = policies.stream()
                .filter(skipNonIdempotentRetries(arguments))
                .map(withRetryListener(arguments));

        @SuppressWarnings("unchecked")
        final Policy<ClientHttpResponse>[] policies = stream.toArray(Policy[]::new);

        return policies;
    }

    private Predicate<Policy<ClientHttpResponse>> skipNonIdempotentRetries(final RequestArguments arguments) {
        return idempotent.test(arguments) ?
                policy -> true :
                policy -> !(policy instanceof RetryPolicy);
    }

    private UnaryOperator<Policy<ClientHttpResponse>> withRetryListener(final RequestArguments arguments) {
        return policy -> {
            if (policy instanceof RetryPolicy) {
                final RetryPolicy<ClientHttpResponse> retryPolicy = (RetryPolicy<ClientHttpResponse>) policy;
                return retryPolicy.copy()
                        .onRetry(new RetryListenerAdapter(listener, arguments));
            } else {
                return policy;
            }
        };
    }

    @VisibleForTesting
    @AllArgsConstructor
    static final class RetryListenerAdapter implements CheckedConsumer<ExecutionAttemptedEvent<ClientHttpResponse>> {
        private final RetryListener listener;
        private final RequestArguments arguments;

        @Override
        public void accept(final ExecutionAttemptedEvent<ClientHttpResponse> event) {
            listener.onRetry(arguments, event);
        }
    }

}

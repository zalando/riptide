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
import org.zalando.riptide.Plugin;
import org.zalando.riptide.RequestArguments;
import org.zalando.riptide.RequestExecution;
import org.zalando.riptide.idempotency.IdempotencyPredicate;

import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import static lombok.AccessLevel.PRIVATE;
import static org.apiguardian.api.API.Status.MAINTAINED;

@API(status = MAINTAINED)
@AllArgsConstructor(access = PRIVATE)
public final class FailsafePlugin implements Plugin {

    private final ImmutableList<? extends Policy<ClientHttpResponse>> policies;
    private final ScheduledExecutorService scheduler;
    private final Predicate<RequestArguments> predicate;
    private final RetryListener listener;

    public FailsafePlugin(final ImmutableList<? extends Policy<ClientHttpResponse>> policies,
            final ScheduledExecutorService scheduler) {
        this(policies, scheduler, new IdempotencyPredicate(), RetryListener.DEFAULT);
    }

    public FailsafePlugin withPredicate(final Predicate<RequestArguments> predicate) {
        return new FailsafePlugin(policies, scheduler, predicate, listener);
    }

    public FailsafePlugin withListener(final RetryListener listener) {
        return new FailsafePlugin(policies, scheduler, predicate, listener);
    }

    @Override
    public RequestExecution aroundDispatch(final RequestExecution execution) {
        return arguments -> {
            final Policy<ClientHttpResponse>[] policies = select(arguments);

            if (policies.length == 0) {
                return execution.execute(arguments);
            }

            return Failsafe.with(select(arguments))
                    .with(scheduler)
                    .getStageAsync(() -> execution.execute(arguments));
        };
    }


    private Policy<ClientHttpResponse>[] select(final RequestArguments arguments) {
        final Stream<Policy<ClientHttpResponse>> stream = policies.stream()
                .filter(skipRetriesIfNeeded(arguments))
                .map(withRetryListener(arguments));

        @SuppressWarnings("unchecked")
        final Policy<ClientHttpResponse>[] policies = stream.toArray(Policy[]::new);

        return policies;
    }

    // TODO depends on the exception, e.g. pre-request exceptions are fine!
    private Predicate<Policy<ClientHttpResponse>> skipRetriesIfNeeded(final RequestArguments arguments) {
        return predicate.test(arguments) ?
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

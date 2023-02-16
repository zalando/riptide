package org.zalando.riptide.failsafe;

import com.google.common.annotations.VisibleForTesting;
import lombok.AllArgsConstructor;
import dev.failsafe.Policy;
import dev.failsafe.RetryPolicy;
import dev.failsafe.event.ExecutionAttemptedEvent;
import dev.failsafe.function.CheckedConsumer;
import org.apiguardian.api.API;
import org.springframework.http.client.ClientHttpResponse;
import org.zalando.riptide.RequestArguments;
import org.zalando.riptide.idempotency.IdempotencyPredicate;

import java.util.function.Predicate;

import static lombok.AccessLevel.PRIVATE;
import static org.apiguardian.api.API.Status.EXPERIMENTAL;

@API(status = EXPERIMENTAL)
@AllArgsConstructor(access = PRIVATE)
public final class RetryRequestPolicy implements RequestPolicy {

    private final RetryPolicy<ClientHttpResponse> policy;
    private final Predicate<RequestArguments> predicate;
    private final RetryListener listener;

    public RetryRequestPolicy(final RetryPolicy<ClientHttpResponse> policy) {
        this(policy, new IdempotencyPredicate(), RetryListener.DEFAULT);
    }

    public RetryRequestPolicy withPredicate(
            final Predicate<RequestArguments> predicate) {
        return new RetryRequestPolicy(policy, predicate, listener);
    }

    public RetryRequestPolicy withListener(final RetryListener listener) {
        return new RetryRequestPolicy(policy, predicate, listener);
    }

    @Override
    public boolean applies(final RequestArguments arguments) {
        return predicate.test(arguments);
    }

    @Override
    public Policy<ClientHttpResponse> prepare(
            final RequestArguments arguments) {

        return policy.copy().onFailedAttempt(
                new RetryListenerAdapter(listener, arguments));
    }

    @VisibleForTesting
    @AllArgsConstructor
    static final class RetryListenerAdapter implements
            CheckedConsumer<ExecutionAttemptedEvent<ClientHttpResponse>> {

        private final RetryListener listener;
        private final RequestArguments arguments;

        @Override
        public void accept(
                final ExecutionAttemptedEvent<ClientHttpResponse> event) {
            listener.onRetry(arguments, event);
        }

    }

}

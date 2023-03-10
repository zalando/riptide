package org.zalando.riptide.failsafe;

import dev.failsafe.Policy;
import dev.failsafe.RetryPolicy;
import org.apiguardian.api.API;
import org.springframework.http.client.ClientHttpResponse;
import org.zalando.riptide.RequestArguments;
import org.zalando.riptide.idempotency.IdempotencyPredicate;

import java.util.function.Predicate;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;

@API(status = EXPERIMENTAL)
public final class RequestPolicies {

    private RequestPolicies() {

    }

    public static RequestPolicy of(final Policy<ClientHttpResponse> policy) {
        if (policy instanceof BackupRequest) {
            return new ConditionalRequestPolicy(
                    new DefaultRequestPolicy(policy),
                    new IdempotencyPredicate());
        } else if (policy instanceof RetryPolicy) {
            return new RetryRequestPolicy(
                    (RetryPolicy<ClientHttpResponse>) policy);
        }

        return new DefaultRequestPolicy(policy);
    }

    public static RequestPolicy of(
            final Policy<ClientHttpResponse> policy,
            final Predicate<RequestArguments> predicate) {
        return of(of(policy), predicate);
    }

    public static RequestPolicy of(
            final RequestPolicy policy,
            final Predicate<RequestArguments> predicate) {
        return new ConditionalRequestPolicy(policy, predicate);
    }
}

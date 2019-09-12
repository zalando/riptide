package org.zalando.riptide.failsafe;

import net.jodah.failsafe.Policy;
import org.apiguardian.api.API;
import org.springframework.http.client.ClientHttpResponse;
import org.zalando.riptide.RequestArguments;

import java.util.function.Predicate;

import static org.apiguardian.api.API.Status.MAINTAINED;

@API(status = MAINTAINED)
public interface RequestPolicy {

    default boolean applies(final RequestArguments arguments) {
        return true;
    }

    Policy<ClientHttpResponse> prepare(final RequestArguments arguments);

    static RequestPolicy of(final Policy<ClientHttpResponse> policy) {
        return new DefaultRequestPolicy(policy);
    }

    static RequestPolicy of(
            final Policy<ClientHttpResponse> policy,
            final Predicate<RequestArguments> predicate) {
        return of(of(policy), predicate);
    }

    static RequestPolicy of(
            final RequestPolicy policy,
            final Predicate<RequestArguments> predicate) {
        return new ConditionalRequestPolicy(policy, predicate);
    }

}

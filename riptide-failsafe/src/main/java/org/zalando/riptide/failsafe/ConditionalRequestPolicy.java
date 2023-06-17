package org.zalando.riptide.failsafe;

import dev.failsafe.Policy;
import lombok.AllArgsConstructor;
import org.springframework.http.client.ClientHttpResponse;
import org.zalando.riptide.RequestArguments;

import java.util.function.Predicate;

@AllArgsConstructor
final class ConditionalRequestPolicy implements RequestPolicy {

    private final RequestPolicy policy;
    private final Predicate<RequestArguments> predicate;

    @Override
    public boolean applies(final RequestArguments arguments) {
        return predicate.test(arguments);
    }

    @Override
    public Policy<ClientHttpResponse> prepare(
            final RequestArguments arguments) {
        return policy.prepare(arguments);
    }

}

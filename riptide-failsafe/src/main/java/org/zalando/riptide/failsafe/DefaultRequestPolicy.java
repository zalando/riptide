package org.zalando.riptide.failsafe;

import lombok.AllArgsConstructor;
import dev.failsafe.Policy;
import org.springframework.http.client.ClientHttpResponse;
import org.zalando.riptide.RequestArguments;

@AllArgsConstructor
final class DefaultRequestPolicy implements RequestPolicy {

    private final Policy<ClientHttpResponse> policy;

    @Override
    public Policy<ClientHttpResponse> prepare(
            final RequestArguments arguments) {
        return policy;
    }

}

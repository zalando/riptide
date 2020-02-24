package org.zalando.riptide.failsafe;

import net.jodah.failsafe.Policy;
import org.apiguardian.api.API;
import org.springframework.http.client.ClientHttpResponse;
import org.zalando.riptide.RequestArguments;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;

@API(status = EXPERIMENTAL)
public interface RequestPolicy {

    default boolean applies(final RequestArguments arguments) {
        return true;
    }

    Policy<ClientHttpResponse> prepare(final RequestArguments arguments);

}

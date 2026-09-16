package org.zalando.riptide.autoconfigure;

import lombok.Getter;
import org.springframework.core.NestedRuntimeException;

@Getter
public class LegacyFailsafeThreadsException extends NestedRuntimeException {

    private final String clientId;
    private final String property;

    public LegacyFailsafeThreadsException(final String clientId, final String property) {
        super(createMessage(clientId, property));
        this.clientId = clientId;
        this.property = property;
    }

    private static String createMessage(final String clientId, final String property) {
        return String.format(
                "Client [%s]: [riptide.clients.%s.%s] is no longer supported, configure " +
                        "[riptide.clients.%s.failsafe.threads] instead",
                clientId, clientId, property, clientId);
    }

}

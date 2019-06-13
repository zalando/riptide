package org.zalando.riptide.idempotency;

import org.junit.jupiter.api.Test;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.zalando.riptide.Http;

import java.util.ServiceConfigurationError;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.fail;

final class IdempotencyPluginTest {
    @Test
    void testHttpCreation() {
        try {
            Http.builder()
                .executor(Executors.newCachedThreadPool())
                .requestFactory(new HttpComponentsClientHttpRequestFactory())
                .build();
        } catch (ServiceConfigurationError e) {
            fail(e.getMessage());
        }
    }
}

package org.zalando.riptide.idempotency;

import org.junit.jupiter.api.*;
import org.springframework.http.client.*;
import org.zalando.riptide.*;

import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

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

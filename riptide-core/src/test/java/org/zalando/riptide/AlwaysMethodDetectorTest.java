package org.zalando.riptide;

import org.junit.jupiter.api.Test;

import static org.springframework.http.HttpMethod.POST;

final class AlwaysMethodDetectorTest {

    private final MethodDetector unit = MethodDetector.always();

    @Test
    void shouldAlwaysDetect() {
        // that's a request that no other detector would test as safe/idempotent
        unit.test(RequestArguments.create().withMethod(POST));
    }

}

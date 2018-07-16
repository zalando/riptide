package org.zalando.riptide;

import org.junit.Test;

import static org.springframework.http.HttpMethod.POST;

public class AlwaysMethodDetectorTest {

    private final MethodDetector unit = MethodDetector.always();

    @Test
    public void shouldAlwaysDetect() {
        // that's a request that no other detector would test as safe/idempotent
        unit.test(RequestArguments.create().withMethod(POST));
    }

}

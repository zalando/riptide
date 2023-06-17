package org.zalando.riptide.idempotency;

import org.junit.jupiter.api.Test;
import org.zalando.riptide.RequestArguments;

import java.util.Collections;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpMethod.POST;

class MethodOverrideIdempotencyDetectorTest {

    @Test
    public void shouldDealWithIncorrectHttpMethodOverride() {
        final RequestArguments arguments = mock(RequestArguments.class);
        final IdempotencyDetector.Test test = mock(IdempotencyDetector.Test.class);

        when(arguments.getMethod()).thenReturn(POST);
        when(arguments.getHeaders()).thenReturn(
                Map.of("X-HTTP-Method-Override", Collections.singletonList("BAD_METHOD"))
        );

        final MethodOverrideIdempotencyDetector methodOverrideIdempotencyDetector
                = new MethodOverrideIdempotencyDetector();

        assertThat(methodOverrideIdempotencyDetector.test(arguments, test), is(Decision.NEUTRAL));
    }

}
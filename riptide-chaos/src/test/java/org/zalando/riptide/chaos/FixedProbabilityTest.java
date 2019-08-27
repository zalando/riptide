package org.zalando.riptide.chaos;

import lombok.extern.slf4j.*;
import org.junit.jupiter.api.*;

import java.util.*;
import java.util.stream.*;

import static java.util.stream.Collectors.*;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

@Slf4j
class FixedProbabilityTest {

    private final Probability unit = Probability.fixed(0.5);

    @Test
    void probability() {
        final Map<Boolean, Long> counts = Stream.generate(unit::test)
                .limit(1_000_000)
                .collect(partitioningBy(test -> test, counting()));

        // difference should be less than 1%
        assertThat(counts.get(true) - counts.get(false), lessThanOrEqualTo(10_000L));
    }

}

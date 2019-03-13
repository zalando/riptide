package org.zalando.riptide.chaos;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.stream.Stream;

import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.partitioningBy;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.lessThanOrEqualTo;

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

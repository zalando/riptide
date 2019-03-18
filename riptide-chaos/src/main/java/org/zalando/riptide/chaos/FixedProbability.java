package org.zalando.riptide.chaos;

import lombok.AllArgsConstructor;
import org.apiguardian.api.API;

import java.util.concurrent.ThreadLocalRandom;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;

@API(status = EXPERIMENTAL)
@AllArgsConstructor
public final class FixedProbability implements Probability {

    private final double probability;

    @Override
    public boolean test() {
        return ThreadLocalRandom.current().nextDouble() < probability;
    }

}

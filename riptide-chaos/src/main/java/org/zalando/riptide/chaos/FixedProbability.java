package org.zalando.riptide.chaos;

import lombok.*;
import org.apiguardian.api.*;

import java.util.concurrent.*;

import static org.apiguardian.api.API.Status.*;

@API(status = EXPERIMENTAL)
@AllArgsConstructor
public final class FixedProbability implements Probability {

    private final double probability;

    @Override
    public boolean test() {
        return ThreadLocalRandom.current().nextDouble() < probability;
    }

}

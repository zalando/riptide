package org.zalando.riptide.chaos;

import org.apiguardian.api.API;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;

@API(status = EXPERIMENTAL)
public interface Probability {

    boolean test();

    static Probability fixed(final double probability) {
        return new FixedProbability(probability);
    }

}

package org.zalando.riptide.chaos;

import org.apiguardian.api.*;

import static org.apiguardian.api.API.Status.*;

@API(status = EXPERIMENTAL)
public interface Probability {

    boolean test();

    static Probability fixed(final double probability) {
        return new FixedProbability(probability);
    }

}

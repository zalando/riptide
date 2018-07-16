package org.zalando.riptide;

import org.apiguardian.api.API;

import java.util.Arrays;
import java.util.Collection;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;

@API(status = EXPERIMENTAL)
@FunctionalInterface
public interface MethodDetector {

    boolean test(RequestArguments arguments);

    static MethodDetector always() {
        return arguments -> true;
    }

    static MethodDetector compound(final MethodDetector... detectors) {
        return compound(Arrays.asList(detectors));
    }

    static MethodDetector compound(final Collection<MethodDetector> detectors) {
        return arguments ->
                detectors.stream().anyMatch(detector -> detector.test(arguments));
    }

}

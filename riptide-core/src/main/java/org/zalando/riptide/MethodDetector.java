package org.zalando.riptide;

import org.apiguardian.api.API;

import java.util.Arrays;
import java.util.Collection;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;

@API(status = EXPERIMENTAL)
@FunctionalInterface
public interface MethodDetector {

    Attribute<Boolean> IDEMPOTENT = Attribute.generate();

    boolean test(RequestArguments arguments);

    static MethodDetector always() {
        return arguments -> true;
    }

    static MethodDetector composite(final MethodDetector... detectors) {
        return composite(Arrays.asList(detectors));
    }

    static MethodDetector composite(final Collection<MethodDetector> detectors) {
        return arguments ->
                detectors.stream().anyMatch(detector -> detector.test(arguments));
    }

}

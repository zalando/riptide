package org.zalando.riptide.faults;

import java.util.function.*;

public final class SelfClassificationStrategy implements ClassificationStrategy {

    @Override
    public boolean test(final Throwable throwable, final Predicate<Throwable> predicate) {
        return predicate.test(throwable);
    }

}

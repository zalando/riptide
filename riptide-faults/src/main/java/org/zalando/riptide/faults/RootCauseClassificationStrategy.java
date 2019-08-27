package org.zalando.riptide.faults;

import com.google.common.base.*;

import java.util.function.Predicate;

public final class RootCauseClassificationStrategy implements ClassificationStrategy {

    @Override
    public boolean test(final Throwable throwable, final Predicate<Throwable> predicate) {
        return predicate.test(Throwables.getRootCause(throwable));
    }

}

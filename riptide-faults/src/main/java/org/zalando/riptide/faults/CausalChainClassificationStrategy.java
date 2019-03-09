package org.zalando.riptide.faults;

import com.google.common.base.Throwables;

import java.util.function.Predicate;

public final class CausalChainClassificationStrategy implements ClassificationStrategy {

    @Override
    @SuppressWarnings("UnstableApiUsage")
    public boolean test(final Throwable throwable, final Predicate<Throwable> predicate) {
        return Throwables.getCausalChain(throwable).stream().anyMatch(predicate);
    }

}

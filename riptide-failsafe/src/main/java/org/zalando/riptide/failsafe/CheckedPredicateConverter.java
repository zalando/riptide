package org.zalando.riptide.failsafe;

import dev.failsafe.function.CheckedPredicate;

import java.util.function.Predicate;

public class CheckedPredicateConverter {

    public static <T> CheckedPredicate<T> toCheckedPredicate(Predicate<T> predicate) {
        return predicate::test;
    }
}

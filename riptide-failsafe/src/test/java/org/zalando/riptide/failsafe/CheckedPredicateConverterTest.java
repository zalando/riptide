package org.zalando.riptide.failsafe;

import dev.failsafe.function.CheckedPredicate;
import org.junit.jupiter.api.Test;

import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.zalando.riptide.failsafe.CheckedPredicateConverter.toCheckedPredicate;

class CheckedPredicateConverterTest {

    @Test
    void shouldConvertToEquivalentPredicate() throws Throwable {
        Predicate<Integer> biggerThan5 = i -> i > 5;
        CheckedPredicate<Integer> checkedPredicate = toCheckedPredicate(biggerThan5);
        assertFalse(checkedPredicate.test(1));
        assertTrue(checkedPredicate.test(10));
    }
}

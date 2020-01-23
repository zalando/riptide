package org.zalando.riptide.faults;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.zalando.riptide.faults.Predicates.alwaysTrue;

class RuleTest {

    @Test
    void defaultsToAcceptAnything() {
        final Rule<String> unit = Rule.of();

        assertTrue(unit.test("anything"));
    }

    @Test
    void allowsToAcceptOnlyIncludes() {
        final Rule<String> unit = Rule.<String>of()
                .include("something"::equals);

        assertTrue(unit.test("something"));
        assertFalse(unit.test("anything"));
    }

    @Test
    void allowsToRejectExcludes() {
        final Rule<String> unit = Rule.of(
                alwaysTrue(), s -> s.endsWith("thing"));

        assertTrue(unit.test("thingy"));
        assertFalse(unit.test("something"));
    }

    @Test
    void allowsToAcceptAndReject() {
        final Rule<String> unit = Rule.of(
                s -> s.endsWith("thing"), s -> s.startsWith("any"));

        assertTrue(unit.test("something"));
        assertFalse(unit.test("anything"));
    }

}

package org.zalando.riptide.failsafe;

import net.jodah.failsafe.util.Duration;

import javax.annotation.Nullable;

interface DelayParser {

    @Nullable
    Duration parse(String value);

}

package org.zalando.riptide.failsafe;

import javax.annotation.Nullable;
import java.time.Duration;

interface DelayParser {

    @Nullable
    Duration parse(String value);

}

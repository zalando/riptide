package org.zalando.riptide.failsafe;

import javax.annotation.*;
import java.time.*;

interface DelayParser {

    @Nullable
    Duration parse(String value);

}

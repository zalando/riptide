package org.zalando.riptide.timeout;

import com.google.gag.annotation.remark.*;
import org.junit.jupiter.api.*;

import java.util.concurrent.*;

import static org.mockito.Mockito.*;

@Hack
@OhNoYouDidnt
final class EnforceCoverageTest {

    @Test
    void shouldUsePrimaryConstructor() {
        new TimeoutPlugin(mock(ScheduledExecutorService.class), 1, TimeUnit.SECONDS);
    }

}

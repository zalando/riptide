package org.zalando.riptide.timeout;

import com.google.gag.annotation.remark.Hack;
import com.google.gag.annotation.remark.OhNoYouDidnt;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.mock;

@Hack
@OhNoYouDidnt
final class EnforceCoverageTest {

    @Test
    void shouldUsePrimaryConstructor() {
        new TimeoutPlugin(mock(ScheduledExecutorService.class), 1, TimeUnit.SECONDS);
    }

}

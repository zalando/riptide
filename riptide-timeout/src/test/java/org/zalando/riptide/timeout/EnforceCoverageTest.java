package org.zalando.riptide.timeout;

import com.google.gag.annotation.remark.Hack;
import com.google.gag.annotation.remark.OhNoYouDidnt;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

@Hack
@OhNoYouDidnt
public final class EnforceCoverageTest {

    @Test
    public void shouldUsePrimaryConstructor() {
        new TimeoutPlugin(1, TimeUnit.SECONDS);
    }

}

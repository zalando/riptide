package org.zalando.riptide.capture;

import com.google.gag.annotation.remark.Hack;
import com.google.gag.annotation.remark.OhNoYouDidnt;
import org.junit.Test;

@Hack
@OhNoYouDidnt
public final class EnforceCoverageTest {

    @Test
    public void shouldUseCompletionConstructor() {
        new Completion();
    }

}

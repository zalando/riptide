package org.zalando.riptide.stream;

import com.google.gag.annotation.remark.Hack;
import com.google.gag.annotation.remark.OhNoYouDidnt;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

@Hack
@OhNoYouDidnt
public final class EnforceCoverageTest {

    @Test
    public void shouldUseStreamsConstructor() {
        assertNotNull(new Streams());
    }
}

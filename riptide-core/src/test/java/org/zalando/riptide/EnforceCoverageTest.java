package org.zalando.riptide;

import com.google.gag.annotation.remark.Hack;
import com.google.gag.annotation.remark.OhNoYouDidnt;
import org.junit.Test;

@Hack
@OhNoYouDidnt
public final class EnforceCoverageTest {

    @Test
    public void shouldUseBindingsConstructor() {
        new Bindings();
    }

    @Test
    public void shouldUseCompletionConstructor() {
        new Completion();
    }

    @Test
    public void shouldUseRestBuilderConvertersConstructor() {
        new RestBuilder.Converters();
    }

    @Test
    public void shouldUseSelectorsConstructor() {
        new Navigators();
    }

}

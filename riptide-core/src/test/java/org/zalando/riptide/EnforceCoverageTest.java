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
    @Deprecated
    public void shouldUseCompletionConstructor() {
        new Completion();
    }

    @Test
    public void shouldUseDefaultHttpBuilderConvertersConstructor() {
        new DefaultHttpBuilder.Converters();
    }

    @Test
    public void shouldUseDefaultHttpBuilderPluginsConstructor() {
        new DefaultHttpBuilder.Plugins();
    }

    @Test
    public void shouldUseSelectorsConstructor() {
        new Navigators();
    }

    @Test
    public void shouldUseTypesConstructor() {
        new Types();
    }

}

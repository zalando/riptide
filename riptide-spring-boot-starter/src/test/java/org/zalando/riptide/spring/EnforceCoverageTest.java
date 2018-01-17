package org.zalando.riptide.spring;

import com.google.gag.annotation.remark.Hack;
import com.google.gag.annotation.remark.OhNoYouDidnt;
import org.junit.Test;

@Hack
@OhNoYouDidnt
public final class EnforceCoverageTest {

    @Test
    public void shouldUseDependenciesConstructor() {
        new Dependencies();
    }

    @Test
    public void shouldUseDefaultingConstructor() {
        new Defaulting();
    }

    @Test
    public void shouldCallHttpFactoryConstructor() {
        new HttpFactory();
    }

    @Test
    public void shouldCallPluginInterceptorsConstructor() {
        new PluginInterceptors();
    }

}

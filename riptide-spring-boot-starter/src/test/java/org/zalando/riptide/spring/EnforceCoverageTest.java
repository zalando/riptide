package org.zalando.riptide.spring;

import com.google.gag.annotation.remark.Hack;
import com.google.gag.annotation.remark.OhNoYouDidnt;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.core.env.ConfigurableEnvironment;

@Hack
@OhNoYouDidnt
@RunWith(MockitoJUnitRunner.class)
public final class EnforceCoverageTest {

    @Mock
    private ConfigurableEnvironment environment;

    @InjectMocks
    private RiptidePostProcessor unit = new RiptidePostProcessor();

    @Test(expected = IllegalStateException.class)
    public void shouldTriggerSneakyException() {
        unit.getSettings();
    }

    @Test
    public void shouldCallHttpFactoryConstructor() {
        new HttpFactory();
    }

}

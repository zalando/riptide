package org.zalando.riptide.spring;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.zalando.riptide.Http;
import org.zalando.riptide.OriginalStackTracePlugin;
import org.zalando.riptide.Plugin;
import org.zalando.riptide.RequestArguments;
import org.zalando.riptide.RequestExecution;
import org.zalando.riptide.backup.BackupRequestPlugin;
import org.zalando.riptide.failsafe.FailsafePlugin;
import org.zalando.riptide.faults.FaultClassifier;
import org.zalando.riptide.faults.TransientFaultPlugin;
import org.zalando.riptide.metrics.MetricsPlugin;
import org.zalando.riptide.timeout.TimeoutPlugin;

import java.lang.reflect.Field;
import java.util.List;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@TestPropertySource(properties = {
        "riptide.clients.bar.detect-transient-faults: true",
})
@Component
public final class PluginTest {

    @Configuration
    @Import(DefaultTestConfiguration.class)
    public static class TestConfiguration {

        @Bean
        public FaultClassifier githubFaultClassifier() {
            return FaultClassifier.createDefault();
        }

        @Bean
        public FaultClassifier faultClassifier() {
            return FaultClassifier.createDefault();
        }

        @Bean
        public Plugin githubPlugin() {
            return new CustomPlugin();
        }

    }

    static class CustomPlugin implements Plugin {

        @Override
        public RequestExecution prepare(final RequestArguments arguments, final RequestExecution execution) {
            return execution;
        }
    }

    @Autowired
    @Qualifier("ecb")
    private Http ecb;

    @Autowired
    @Qualifier("foo")
    private Http foo;

    @Autowired
    @Qualifier("baz")
    private Http baz;

    @Autowired
    @Qualifier("github")
    private Http github;

    @Autowired
    @Qualifier("example")
    private Http example;

    @Test
    public void shouldUseTransientFaultPlugin() throws Exception {
        assertThat(getPlugins(github), contains(asList(
                instanceOf(MetricsPlugin.class),
                instanceOf(TransientFaultPlugin.class),
                instanceOf(CustomPlugin.class))));
    }

    @Test
    public void shouldUseFailsafePlugin() throws Exception {
        assertThat(getPlugins(foo), contains(asList(
                instanceOf(MetricsPlugin.class),
                instanceOf(FailsafePlugin.class))));
    }

    @Test
    public void shouldUseBackupRequestPlugin() throws Exception {
        assertThat(getPlugins(baz), contains(asList(
                instanceOf(MetricsPlugin.class),
                instanceOf(BackupRequestPlugin.class))));
    }

    @Test
    public void shouldUseTimeoutPlugin() throws Exception {
        assertThat(getPlugins(ecb), contains(asList(
                instanceOf(MetricsPlugin.class),
                instanceOf(TimeoutPlugin.class))));
    }

    @Test
    public void shouldUseOriginalStackTracePlugin() throws Exception {
        assertThat(getPlugins(example), contains(asList(
                instanceOf(MetricsPlugin.class),
                instanceOf(OriginalStackTracePlugin.class))));
    }

    private List<Plugin> getPlugins(final Http http) throws Exception {
        final Field field = http.getClass().getDeclaredField("plugin");
        field.setAccessible(true);

        @SuppressWarnings("unchecked") final Plugin plugin = (Plugin) field.get(http);

        final Field plugins = plugin.getClass().getDeclaredField("plugins");
        plugins.setAccessible(true);

        @SuppressWarnings("unchecked") final List<Plugin> list = (List<Plugin>) plugins.get(plugin);

        return list;
    }

}

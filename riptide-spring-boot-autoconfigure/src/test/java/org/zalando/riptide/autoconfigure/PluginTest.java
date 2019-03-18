package org.zalando.riptide.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;
import org.springframework.test.context.TestPropertySource;
import org.zalando.riptide.Http;
import org.zalando.riptide.OriginalStackTracePlugin;
import org.zalando.riptide.Plugin;
import org.zalando.riptide.backup.BackupRequestPlugin;
import org.zalando.riptide.chaos.ChaosPlugin;
import org.zalando.riptide.failsafe.FailsafePlugin;
import org.zalando.riptide.faults.DefaultFaultClassifier;
import org.zalando.riptide.faults.FaultClassifier;
import org.zalando.riptide.faults.TransientFaultPlugin;
import org.zalando.riptide.metrics.MetricsPlugin;
import org.zalando.riptide.timeout.TimeoutPlugin;

import java.lang.reflect.Field;
import java.util.List;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.instanceOf;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

@SpringBootTest(classes = PluginTest.TestConfiguration.class, webEnvironment = NONE)
@TestPropertySource(properties = {
        "riptide.clients.bar.transient-fault-detection.enabled: true",
})
@Component
final class PluginTest {

    @Configuration
    @Import(DefaultTestConfiguration.class)
    public static class TestConfiguration {

        @Bean
        public FaultClassifier githubFaultClassifier() {
            return new DefaultFaultClassifier();
        }

        @Bean
        public FaultClassifier faultClassifier() {
            return new DefaultFaultClassifier();
        }

        @Bean
        public Plugin githubPlugin() {
            return new CustomPlugin();
        }

    }

    static class CustomPlugin implements Plugin {

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
    void shouldUseTransientFaultPlugin() throws Exception {
        assertThat(getPlugins(github), contains(asList(
                instanceOf(Plugin.class), // internal plugin
                instanceOf(Plugin.class), // internal plugin
                instanceOf(MetricsPlugin.class),
                instanceOf(TransientFaultPlugin.class),
                instanceOf(CustomPlugin.class))));
    }

    @Test
    void shouldUseFailsafePlugin() throws Exception {
        assertThat(getPlugins(foo), contains(asList(
                instanceOf(Plugin.class), // internal plugin
                instanceOf(Plugin.class), // internal plugin
                instanceOf(MetricsPlugin.class),
                instanceOf(FailsafePlugin.class))));
    }

    @Test
    void shouldUseBackupRequestPlugin() throws Exception {
        assertThat(getPlugins(baz), contains(asList(
                instanceOf(Plugin.class), // internal plugin
                instanceOf(Plugin.class), // internal plugin
                instanceOf(MetricsPlugin.class),
                instanceOf(BackupRequestPlugin.class))));
    }

    @Test
    void shouldUseTimeoutPlugin() throws Exception {
        assertThat(getPlugins(ecb), contains(asList(
                instanceOf(Plugin.class), // internal plugin
                instanceOf(Plugin.class), // internal plugin
                instanceOf(ChaosPlugin.class),
                instanceOf(MetricsPlugin.class),
                instanceOf(TimeoutPlugin.class))));
    }

    @Test
    void shouldUseOriginalStackTracePlugin() throws Exception {
        assertThat(getPlugins(example), contains(asList(
                instanceOf(Plugin.class), // internal plugin
                instanceOf(Plugin.class), // internal plugin
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

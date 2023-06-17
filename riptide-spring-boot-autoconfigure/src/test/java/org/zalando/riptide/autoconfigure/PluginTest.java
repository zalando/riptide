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
import org.zalando.riptide.failsafe.FailsafePlugin;
import org.zalando.riptide.logbook.LogbookPlugin;
import org.zalando.riptide.micrometer.MicrometerPlugin;
import org.zalando.riptide.opentelemetry.OpenTelemetryPlugin;
import org.zalando.riptide.opentracing.OpenTracingPlugin;

import java.lang.reflect.Field;
import java.util.List;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
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
    void shouldUseFailsafePlugin() throws Exception {
        assertThat(getPlugins(foo), contains(asList(
                instanceOf(Plugin.class), // internal plugin
                instanceOf(Plugin.class), // internal plugin
                instanceOf(Plugin.class), // internal plugin
                instanceOf(MicrometerPlugin.class),
                instanceOf(OpenTracingPlugin.class),
                instanceOf(FailsafePlugin.class))));
    }

    @Test
    void shouldUseBackupRequestPlugin() throws Exception {
        assertThat(getPlugins(baz), contains(asList(
                instanceOf(Plugin.class), // internal plugin
                instanceOf(Plugin.class), // internal plugin
                instanceOf(Plugin.class), // internal plugin
                instanceOf(MicrometerPlugin.class),
                instanceOf(FailsafePlugin.class), // backup requests
                instanceOf(FailsafePlugin.class)))); // timeouts
    }

    @Test
    void shouldUseOriginalStackTracePlugin() throws Exception {
        assertThat(getPlugins(example), contains(asList(
                instanceOf(Plugin.class), // internal plugin
                instanceOf(Plugin.class), // internal plugin
                instanceOf(Plugin.class), // internal plugin
                instanceOf(MicrometerPlugin.class),
                instanceOf(LogbookPlugin.class),
                instanceOf(OpenTracingPlugin.class),
                instanceOf(OriginalStackTracePlugin.class))));
    }

    @Test
    void shouldUseOpenTelemetryPlugin() throws Exception {
        assertThat(getPlugins(github), hasItem(instanceOf(OpenTelemetryPlugin.class)));
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

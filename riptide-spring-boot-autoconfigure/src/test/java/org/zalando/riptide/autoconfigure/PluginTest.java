package org.zalando.riptide.autoconfigure;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.*;
import org.springframework.context.annotation.*;
import org.springframework.stereotype.*;
import org.springframework.test.context.*;
import org.zalando.riptide.*;
import org.zalando.riptide.backup.*;
import org.zalando.riptide.chaos.*;
import org.zalando.riptide.failsafe.*;
import org.zalando.riptide.faults.*;
import org.zalando.riptide.logbook.*;
import org.zalando.riptide.micrometer.*;
import org.zalando.riptide.opentracing.*;
import org.zalando.riptide.timeout.*;

import java.lang.reflect.*;
import java.util.*;

import static java.util.Arrays.*;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.*;

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
                instanceOf(Plugin.class), // internal plugin
                instanceOf(MicrometerPlugin.class),
                instanceOf(TransientFaultPlugin.class),
                instanceOf(CustomPlugin.class))));
    }

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
                instanceOf(BackupRequestPlugin.class))));
    }

    @Test
    void shouldUseTimeoutPlugin() throws Exception {
        assertThat(getPlugins(ecb), contains(asList(
                instanceOf(Plugin.class), // internal plugin
                instanceOf(Plugin.class), // internal plugin
                instanceOf(Plugin.class), // internal plugin
                instanceOf(ChaosPlugin.class),
                instanceOf(MicrometerPlugin.class),
                instanceOf(RequestCompressionPlugin.class),
                instanceOf(TimeoutPlugin.class))));
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

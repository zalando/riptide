package org.zalando.riptide.spring;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.TestPropertySource;
import org.zalando.riptide.timeout.TimeoutPlugin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Import;
import org.springframework.http.client.AsyncClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.AsyncRestTemplate;
import org.zalando.riptide.Http;
import org.zalando.riptide.OriginalStackTracePlugin;
import org.zalando.riptide.Plugin;
import org.zalando.riptide.failsafe.FailsafePlugin;
import org.zalando.riptide.faults.FaultClassifier;
import org.zalando.riptide.faults.TransientFaultPlugin;

import java.lang.reflect.Field;
import java.util.List;

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
        public FaultClassifier ecbFaultClassifier() {
            return FaultClassifier.createDefault();
        }

        @Bean
        public FaultClassifier faultClassifier() {
            return FaultClassifier.createDefault();
        }

        @Bean
        public AsyncRestTemplate template() {
            return new AsyncRestTemplate();
        }

        @Bean
        public MockRestServiceServer server(final AsyncRestTemplate template) {
            return MockRestServiceServer.createServer(template);
        }

        @Bean
        @DependsOn("server")
        public AsyncClientHttpRequestFactory exampleAsyncClientHttpRequestFactory(final AsyncRestTemplate template) {
            return template.getAsyncRequestFactory();
        }

        @Bean
        @DependsOn("server")
        public AsyncClientHttpRequestFactory fooAsyncClientHttpRequestFactory(final AsyncRestTemplate template) {
            return template.getAsyncRequestFactory();
        }

    }

    @Autowired
    private MockRestServiceServer server;

    @Autowired
    @Qualifier("ecb")
    private Http ecb;

    @Autowired
    @Qualifier("foo")
    private Http foo;

    @Autowired
    @Qualifier("github")
    private Http github;

    @Autowired
    @Qualifier("example")
    private Http example;

    @Test
    public void shouldUseTransientFaultPlugin() throws Exception {
        assertThat(getPlugins(ecb), contains(instanceOf(TransientFaultPlugin.class)));
    }

    @Test
    public void shouldUseFailsafePlugin() throws Exception {
        assertThat(getPlugins(foo), contains(instanceOf(FailsafePlugin.class)));
    }

    public void shouldUseTimeoutPlugin() throws Exception {
        assertThat(getPlugins(github), contains(instanceOf(TimeoutPlugin.class)));
    }

    @Test
    public void shouldUseOriginalStackTracePlugin() throws Exception {
        assertThat(getPlugins(example), contains(instanceOf(OriginalStackTracePlugin.class)));
    }

    private List<Plugin> getPlugins(final Http http) throws Exception {
        final Field field = http.getClass().getDeclaredField("plugins");
        field.setAccessible(true);

        @SuppressWarnings("unchecked")
        final List<Plugin> plugins = (List<Plugin>) field.get(http);

        return plugins;
    }

}

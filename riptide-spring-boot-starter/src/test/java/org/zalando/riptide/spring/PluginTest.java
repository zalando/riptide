package org.zalando.riptide.spring;

import org.junit.Test;
import org.junit.runner.RunWith;
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
import org.zalando.riptide.exceptions.TemporaryExceptionPlugin;
import org.zalando.riptide.failsafe.FailsafePlugin;

import java.lang.reflect.Field;
import java.util.List;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@Component
public final class PluginTest {

    @Configuration
    @Import(DefaultTestConfiguration.class)
    public static class TestConfiguration {

        @Bean
        public TemporaryExceptionPlugin temporaryExceptionPlugin() {
            return new TemporaryExceptionPlugin();
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
    @Qualifier("example")
    private Http example;

    @Autowired
    @Qualifier("ecb")
    private Http ecb;

    @Autowired
    @Qualifier("foo")
    private Http foo;

    @Test
    public void shouldUseOriginalStackTracePlugin() throws Exception {
        assertThat(getPlugins(example), contains(instanceOf(OriginalStackTracePlugin.class)));
    }

    @Test
    public void shouldUseTemporaryExceptionPlugin() throws Exception {
        assertThat(getPlugins(ecb), contains(instanceOf(TemporaryExceptionPlugin.class)));
    }

    @Test
    public void shouldUseFailsafeException() throws Exception {
        assertThat(getPlugins(foo), contains(instanceOf(FailsafePlugin.class)));
    }

    private List<Plugin> getPlugins(final Http http) throws Exception {
        final Field field = http.getClass().getDeclaredField("plugins");
        field.setAccessible(true);

        @SuppressWarnings("unchecked")
        final List<Plugin> plugins = (List<Plugin>) field.get(http);

        return plugins;
    }

}

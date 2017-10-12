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
import org.zalando.riptide.hystrix.HystrixPlugin;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.zalando.riptide.PassRoute.pass;

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
    public void shouldUseDefault() throws Exception {
        assertThat(getPlugins(example), contains(equalTo(OriginalStackTracePlugin.class)));
    }

    @Test
    public void shouldUseCreatedPlugin() {
        server.expect(requestTo("http://localhost")).andRespond(withSuccess());
        example.get("http://localhost").call(pass()).join();
    }

    @Test
    public void shouldUseTemporaryException() throws Exception {
        assertThat(getPlugins(ecb), contains(equalTo(TemporaryExceptionPlugin.class)));
    }

    @Test
    public void shouldUseProvidedPlugin() {
        server.expect(requestTo("http://localhost")).andRespond(withSuccess());
        foo.get("http://localhost").call(pass()).join();
    }

    private List<Class<? extends Plugin>> getPlugins(final Http http) throws Exception {
        final List<Class<? extends Plugin>> plugins = new ArrayList<>();

        final Field field = http.getClass().getDeclaredField("plugin");
        field.setAccessible(true);

        final Plugin plugin = (Plugin) field.get(http);

        plugins.add(plugin.getClass());

        return plugins;
    }

}

package org.zalando.riptide.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import net.jodah.failsafe.CircuitBreaker;
import net.jodah.failsafe.RetryPolicy;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.ssl.SSLContexts;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.task.AsyncListenableTaskExecutor;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.zalando.logbook.Logbook;
import org.zalando.logbook.httpclient.LogbookHttpRequestInterceptor;
import org.zalando.logbook.httpclient.LogbookHttpResponseInterceptor;
import org.zalando.logbook.spring.LogbookAutoConfiguration;
import org.zalando.riptide.Http;
import org.zalando.riptide.OriginalStackTracePlugin;
import org.zalando.riptide.Plugin;
import org.zalando.riptide.PluginInterceptor;
import org.zalando.riptide.UrlResolution;
import org.zalando.riptide.auth.AuthorizationPlugin;
import org.zalando.riptide.auth.PlatformCredentialsAuthorizationProvider;
import org.zalando.riptide.autoconfigure.PluginTest.CustomPlugin;
import org.zalando.riptide.backup.BackupRequestPlugin;
import org.zalando.riptide.chaos.ChaosPlugin;
import org.zalando.riptide.chaos.ErrorResponseInjection;
import org.zalando.riptide.chaos.ExceptionInjection;
import org.zalando.riptide.chaos.LatencyInjection;
import org.zalando.riptide.chaos.Probability;
import org.zalando.riptide.failsafe.CircuitBreakerListener;
import org.zalando.riptide.failsafe.FailsafePlugin;
import org.zalando.riptide.failsafe.RetryAfterDelayFunction;
import org.zalando.riptide.failsafe.RetryException;
import org.zalando.riptide.failsafe.metrics.MetricsCircuitBreakerListener;
import org.zalando.riptide.failsafe.metrics.MetricsRetryListener;
import org.zalando.riptide.faults.TransientFaultException;
import org.zalando.riptide.faults.TransientFaultPlugin;
import org.zalando.riptide.httpclient.ApacheClientHttpRequestFactory;
import org.zalando.riptide.httpclient.GzipHttpRequestInterceptor;
import org.zalando.riptide.idempotency.IdempotencyPredicate;
import org.zalando.riptide.metrics.MetricsPlugin;
import org.zalando.riptide.soap.SOAPFaultHttpMessageConverter;
import org.zalando.riptide.soap.SOAPHttpMessageConverter;
import org.zalando.riptide.stream.Streams;
import org.zalando.riptide.timeout.TimeoutPlugin;
import org.zalando.tracer.Tracer;
import org.zalando.tracer.concurrent.TracingExecutors;
import org.zalando.tracer.httpclient.TracerHttpRequestInterceptor;
import org.zalando.tracer.spring.TracerAutoConfiguration;

import java.net.SocketTimeoutException;
import java.time.Clock;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.AbortPolicy;

import static java.time.temporal.ChronoUnit.MILLIS;
import static java.util.Collections.singletonList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toList;
import static org.apache.http.conn.ssl.SSLConnectionSocketFactory.getDefaultHostnameVerifier;
import static org.zalando.riptide.chaos.FailureInjection.composite;

@Configuration
public class ManualConfiguration {

    @Configuration
    // just for documentation, should not be imported manually
    @Import({
            LogbookAutoConfiguration.class,
            TracerAutoConfiguration.class,
            JacksonAutoConfiguration.class,
            MetricsAutoConfiguration.class,
    })
    static class ExampleClientConfiguration {

        @Bean
        public Http exampleHttp(final Executor executor, final ClientHttpRequestFactory requestFactory,
                final ClientHttpMessageConverters converters, final List<Plugin> plugins) {

            return Http.builder()
                    .executor(executor)
                    .requestFactory(requestFactory)
                    .baseUrl("https://www.example.com")
                    .urlResolution(UrlResolution.RFC)
                    .converters(converters.getConverters())
                    .plugins(plugins)
                    .build();
        }

        @Bean
        public List<Plugin> examplePlugins(final MeterRegistry meterRegistry,
                final ScheduledExecutorService scheduler, final Executor executor) {

            final CircuitBreakerListener listener = new MetricsCircuitBreakerListener(meterRegistry)
                    .withDefaultTags(Tag.of("clientId", "example"));

            return Arrays.asList(
                    new ChaosPlugin(composite(
                            new LatencyInjection(
                                    Probability.fixed(0.01),
                                    Clock.systemUTC(),
                                    Duration.ofSeconds(1)),
                            new ExceptionInjection(
                                    Probability.fixed(0.001),
                                    singletonList(SocketTimeoutException::new)),
                            new ErrorResponseInjection(
                                    Probability.fixed(0.001),
                                    Arrays.asList(
                                            HttpStatus.INTERNAL_SERVER_ERROR,
                                            HttpStatus.SERVICE_UNAVAILABLE)))),
                    new MetricsPlugin(meterRegistry)
                            .withDefaultTags(Tag.of("clientId", "example")),
                    new TransientFaultPlugin(),
                    new FailsafePlugin(
                            ImmutableList.of(
                                    new RetryPolicy<ClientHttpResponse>()
                                            .handle(TransientFaultException.class)
                                            .handle(RetryException.class)
                                            .withBackoff(50, 2000, MILLIS)
                                            .withDelay(new RetryAfterDelayFunction(Clock.systemUTC()))
                                            .withMaxRetries(10)
                                            .withMaxDuration(Duration.ofSeconds(2))
                                            .withJitter(0.2),
                                    new CircuitBreaker<ClientHttpResponse>()
                                            .withFailureThreshold(5, 5)
                                            .withDelay(Duration.ofSeconds(30))
                                            .withSuccessThreshold(3, 5)
                                            .withTimeout(Duration.ofSeconds(3))
                                            .onOpen(listener::onOpen)
                                            .onHalfOpen(listener::onHalfOpen)
                                            .onClose(listener::onClose)
                            ),
                            scheduler)
                            .withPredicate(new IdempotencyPredicate())
                            .withListener(new MetricsRetryListener(meterRegistry)
                                    .withDefaultTags(Tag.of("clientId", "example"))),
                    new BackupRequestPlugin(scheduler, 10, MILLISECONDS)
                            .withExecutor(executor)
                            .withPredicate(new IdempotencyPredicate()),
                    new AuthorizationPlugin(new PlatformCredentialsAuthorizationProvider("example")),
                    new TimeoutPlugin(scheduler, 3, SECONDS, executor),
                    new OriginalStackTracePlugin(),
                    new CustomPlugin());
        }

        @Bean
        public RestTemplate exampleRestTemplate(final ClientHttpRequestFactory requestFactory,
                final ClientHttpMessageConverters converters, final List<Plugin> plugins) {
            final RestTemplate template = new RestTemplate();

            final DefaultUriBuilderFactory handler = new DefaultUriBuilderFactory("https://www.example.com");
            template.setUriTemplateHandler(handler);
            template.setRequestFactory(requestFactory);
            template.setMessageConverters(converters.getConverters());
            template.setInterceptors(plugins.stream().map(PluginInterceptor::new).collect(toList()));

            return template;
        }

        @Bean
        public AsyncRestTemplate exampleAsyncRestTemplate(final ClientHttpRequestFactory requestFactory,
                final Executor executor, final ClientHttpMessageConverters converters, final List<Plugin> plugins) {
            final AsyncRestTemplate template = new AsyncRestTemplate();

            final AsyncListenableTaskExecutor taskExecutor = new ConcurrentTaskExecutor(executor);
            final DefaultUriBuilderFactory handler = new DefaultUriBuilderFactory("https://www.example.com");
            template.setUriTemplateHandler(handler);
            template.setAsyncRequestFactory(new ConcurrentClientHttpRequestFactory(requestFactory, taskExecutor));
            template.setMessageConverters(converters.getConverters());
            template.setInterceptors(plugins.stream().map(PluginInterceptor::new).collect(toList()));

            return template;
        }

        @Bean
        public ApacheClientHttpRequestFactory exampleAsyncClientHttpRequestFactory(
                final Tracer tracer, final Logbook logbook) throws Exception {
            return new ApacheClientHttpRequestFactory(
                    HttpClientBuilder.create()
                            .setDefaultRequestConfig(RequestConfig.custom()
                                    .setConnectTimeout(5000)
                                    .setSocketTimeout(5000)
                                    .build())
                            .setConnectionTimeToLive(30, SECONDS)
                            .setMaxConnPerRoute(2)
                            .setMaxConnTotal(20)
                            .addInterceptorFirst(new TracerHttpRequestInterceptor(tracer))
                            .addInterceptorLast(new LogbookHttpRequestInterceptor(logbook))
                            .addInterceptorLast(new GzipHttpRequestInterceptor())
                            .addInterceptorLast(new LogbookHttpResponseInterceptor())
                            .setSSLSocketFactory(new SSLConnectionSocketFactory(
                                    SSLContexts.custom()
                                            .loadTrustMaterial(
                                                    getClass().getClassLoader().getResource("example.keystore"),
                                                    "password".toCharArray())
                                            .build(),
                                    getDefaultHostnameVerifier()))
                            .build());
        }

        @Bean(destroyMethod = "shutdown")
        public ExecutorService executor(final Tracer tracer) {
            return TracingExecutors.preserve(
                    new ThreadPoolExecutor(
                            1, 20, 1, MINUTES,
                            new ArrayBlockingQueue<>(0),
                            new CustomizableThreadFactory("http-example-"),
                            new AbortPolicy()),
                    tracer);
        }

        @Bean(destroyMethod = "shutdown")
        public ScheduledExecutorService scheduler(final Tracer tracer) {
            return TracingExecutors.preserve(
                    Executors.newScheduledThreadPool(
                            20, // TODO max-connections-total?
                            new CustomizableThreadFactory("http-example-scheduler-")),
                    tracer);
        }

        @Bean
        public ClientHttpMessageConverters exampleHttpMessageConverters(final ObjectMapper mapper) {
            final StringHttpMessageConverter textConverter = new StringHttpMessageConverter();
            textConverter.setWriteAcceptCharset(false);

            return new ClientHttpMessageConverters(Arrays.asList(
                    new MappingJackson2HttpMessageConverter(mapper),
                    Streams.streamConverter(mapper),
                    new SOAPHttpMessageConverter(),
                    new SOAPFaultHttpMessageConverter(),
                    textConverter
            ));
        }

    }

}

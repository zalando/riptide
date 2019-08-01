package org.zalando.riptide.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics;
import io.opentracing.Tracer;
import io.opentracing.contrib.concurrent.TracedExecutorService;
import io.opentracing.contrib.concurrent.TracedScheduledExecutorService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.web.client.AsyncRestOperations;
import org.springframework.web.client.RestOperations;
import org.zalando.logbook.Logbook;
import org.zalando.logbook.autoconfigure.LogbookAutoConfiguration;
import org.zalando.riptide.Http;
import org.zalando.riptide.OriginalStackTracePlugin;
import org.zalando.riptide.Plugin;
import org.zalando.riptide.RequestCompressionPlugin;
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
import org.zalando.riptide.compatibility.AsyncHttpOperations;
import org.zalando.riptide.compatibility.HttpOperations;
import org.zalando.riptide.failsafe.CircuitBreakerListener;
import org.zalando.riptide.failsafe.CompositeDelayFunction;
import org.zalando.riptide.failsafe.FailsafePlugin;
import org.zalando.riptide.failsafe.RateLimitResetDelayFunction;
import org.zalando.riptide.failsafe.RetryAfterDelayFunction;
import org.zalando.riptide.failsafe.RetryException;
import org.zalando.riptide.failsafe.metrics.MetricsCircuitBreakerListener;
import org.zalando.riptide.failsafe.metrics.MetricsRetryListener;
import org.zalando.riptide.faults.TransientFaultException;
import org.zalando.riptide.faults.TransientFaultPlugin;
import org.zalando.riptide.httpclient.ApacheClientHttpRequestFactory;
import org.zalando.riptide.idempotency.IdempotencyPredicate;
import org.zalando.riptide.logbook.LogbookPlugin;
import org.zalando.riptide.micrometer.MicrometerPlugin;
import org.zalando.riptide.opentracing.OpenTracingPlugin;
import org.zalando.riptide.soap.SOAPFaultHttpMessageConverter;
import org.zalando.riptide.soap.SOAPHttpMessageConverter;
import org.zalando.riptide.stream.Streams;
import org.zalando.riptide.timeout.TimeoutPlugin;
import org.zalando.tracer.Flow;
import org.zalando.tracer.autoconfigure.TracerAutoConfiguration;
import org.zalando.tracer.httpclient.FlowHttpRequestInterceptor;

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

import static java.time.Clock.systemUTC;
import static java.time.temporal.ChronoUnit.MILLIS;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static javax.net.ssl.HttpsURLConnection.getDefaultHostnameVerifier;
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
        public List<Plugin> examplePlugins(final MeterRegistry meterRegistry, final Logbook logbook,
                final Tracer tracer, final ScheduledExecutorService scheduler, final Executor executor) {

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
                    new MicrometerPlugin(meterRegistry)
                            .withDefaultTags(Tag.of("clientId", "example")),
                    new RequestCompressionPlugin(),
                    new LogbookPlugin(logbook),
                    new TransientFaultPlugin(),
                    new OpenTracingPlugin(tracer),
                    new FailsafePlugin(
                            ImmutableList.of(
                                    new RetryPolicy<ClientHttpResponse>()
                                            .handle(TransientFaultException.class)
                                            .handle(RetryException.class)
                                            .withBackoff(50, 2000, MILLIS)
                                            .withDelay(new CompositeDelayFunction<>(Arrays.asList(
                                                    new RetryAfterDelayFunction(systemUTC()),
                                                    new RateLimitResetDelayFunction(systemUTC())
                                            )))
                                            .withMaxRetries(10)
                                            .withMaxDuration(Duration.ofSeconds(2))
                                            .withJitter(0.2),
                                    new CircuitBreaker<ClientHttpResponse>()
                                            .withFailureThreshold(5, 5)
                                            .withDelay(Duration.ofSeconds(30))
                                            .withSuccessThreshold(3, 5)
                                            .withTimeout(Duration.ofSeconds(3))
                                            .withDelay(new CompositeDelayFunction<>(Arrays.asList(
                                                    new RetryAfterDelayFunction(systemUTC()),
                                                    new RateLimitResetDelayFunction(systemUTC())
                                            )))
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
        public RestOperations exampleRestOperations(final Http http) {
            return new HttpOperations(http);
        }

        @Bean
        public AsyncRestOperations exampleAsyncRestOperations(final Http http) {
            return new AsyncHttpOperations(http);
        }

        @Bean
        public ApacheClientHttpRequestFactory exampleAsyncClientHttpRequestFactory(
                final Flow flow) throws Exception {
            return new ApacheClientHttpRequestFactory(
                    HttpClientBuilder.create()
                            .setDefaultRequestConfig(RequestConfig.custom()
                                    .setConnectTimeout(5000)
                                    .setSocketTimeout(5000)
                                    .build())
                            .setConnectionTimeToLive(30, SECONDS)
                            .setMaxConnPerRoute(2)
                            .setMaxConnTotal(20)
                            .addInterceptorFirst(new FlowHttpRequestInterceptor(flow))
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
            return new TracedExecutorService(
                    new ThreadPoolExecutor(
                            1, 20, 1, MINUTES,
                            new ArrayBlockingQueue<>(0),
                            new CustomizableThreadFactory("http-example-"),
                            new AbortPolicy()),
                    tracer);
        }

        @Bean
        public MeterBinder executorServiceMetrics(final ExecutorService executor) {
            return new ExecutorServiceMetrics(executor, "http-example", singleton(Tag.of("clientId", "example")));
        }

        @Bean(destroyMethod = "shutdown")
        public ScheduledExecutorService scheduler(final Tracer tracer) {
            return new TracedScheduledExecutorService(
                    Executors.newScheduledThreadPool(
                            20,
                            new CustomizableThreadFactory("http-example-scheduler-")),
                    tracer);
        }

        @Bean
        public MeterBinder scheduledExecutorServiceMetrics(final ScheduledExecutorService executor) {
            return new ExecutorServiceMetrics(executor, "http-example-scheduler", singleton(Tag.of("clientId", "example")));
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

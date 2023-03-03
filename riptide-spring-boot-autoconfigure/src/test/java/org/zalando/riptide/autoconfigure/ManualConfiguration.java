package org.zalando.riptide.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics;
import io.opentracing.Tracer;
import io.opentracing.contrib.concurrent.TracedExecutorService;
import net.jodah.failsafe.CircuitBreaker;
import net.jodah.failsafe.RetryPolicy;
import net.jodah.failsafe.Timeout;
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
import org.zalando.opentracing.flowid.Flow;
import org.zalando.opentracing.flowid.autoconfigure.OpenTracingFlowIdAutoConfiguration;
import org.zalando.opentracing.flowid.httpclient.FlowHttpRequestInterceptor;
import org.zalando.riptide.Http;
import org.zalando.riptide.OriginalStackTracePlugin;
import org.zalando.riptide.Plugin;
import org.zalando.riptide.UrlResolution;
import org.zalando.riptide.auth.AuthorizationPlugin;
import org.zalando.riptide.auth.PlatformCredentialsAuthorizationProvider;
import org.zalando.riptide.autoconfigure.PluginTest.CustomPlugin;
import org.zalando.riptide.chaos.ChaosPlugin;
import org.zalando.riptide.chaos.ErrorResponseInjection;
import org.zalando.riptide.chaos.ExceptionInjection;
import org.zalando.riptide.chaos.LatencyInjection;
import org.zalando.riptide.chaos.Probability;
import org.zalando.riptide.compatibility.HttpOperations;
import org.zalando.riptide.compression.RequestCompressionPlugin;
import org.zalando.riptide.concurrent.ThreadPoolExecutors;
import org.zalando.riptide.failsafe.BackupRequest;
import org.zalando.riptide.failsafe.CircuitBreakerListener;
import org.zalando.riptide.failsafe.CompositeDelayFunction;
import org.zalando.riptide.failsafe.FailsafePlugin;
import org.zalando.riptide.failsafe.RateLimitResetDelayFunction;
import org.zalando.riptide.failsafe.RetryAfterDelayFunction;
import org.zalando.riptide.failsafe.RetryRequestPolicy;
import org.zalando.riptide.failsafe.metrics.MetricsCircuitBreakerListener;
import org.zalando.riptide.httpclient.ApacheClientHttpRequestFactory;
import org.zalando.riptide.idempotency.IdempotencyPredicate;
import org.zalando.riptide.logbook.LogbookPlugin;
import org.zalando.riptide.micrometer.MicrometerPlugin;
import org.zalando.riptide.micrometer.tag.RetryTagGenerator;
import org.zalando.riptide.opentracing.OpenTracingPlugin;
import org.zalando.riptide.opentracing.TracedTaskDecorator;
import org.zalando.riptide.soap.SOAPFaultHttpMessageConverter;
import org.zalando.riptide.soap.SOAPHttpMessageConverter;
import org.zalando.riptide.stream.Streams;

import java.net.SocketTimeoutException;
import java.time.Clock;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.function.Predicate;

import static java.time.Clock.systemUTC;
import static java.time.temporal.ChronoUnit.MILLIS;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static javax.net.ssl.HttpsURLConnection.getDefaultHostnameVerifier;
import static org.zalando.riptide.chaos.FailureInjection.composite;
import static org.zalando.riptide.faults.Predicates.alwaysTrue;
import static org.zalando.riptide.faults.TransientFaults.transientConnectionFaults;
import static org.zalando.riptide.faults.TransientFaults.transientSocketFaults;

@Configuration
public class ManualConfiguration {

    @Configuration
    // just for documentation, should not be imported manually
    @Import({
            LogbookAutoConfiguration.class,
            OpenTracingFlowIdAutoConfiguration.class,
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
                final Tracer tracer) {

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
                            .withDefaultTags(Tag.of("clientId", "example"))
                            .withAdditionalTagGenerators(new RetryTagGenerator()),
                    new RequestCompressionPlugin(),
                    new LogbookPlugin(logbook),
                    new OpenTracingPlugin(tracer),
                    new FailsafePlugin()
                            .withPolicy(new CircuitBreaker<ClientHttpResponse>()
                                    .withFailureThreshold(5, 5)
                                    .withDelay(Duration.ofSeconds(30))
                                    .withSuccessThreshold(3, 5)
                                    .withDelay(new CompositeDelayFunction<>(Arrays.asList(
                                            new RetryAfterDelayFunction(systemUTC()),
                                            new RateLimitResetDelayFunction(systemUTC())
                                    )))
                                    .onOpen(listener::onOpen)
                                    .onHalfOpen(listener::onHalfOpen)
                                    .onClose(listener::onClose))
                            .withDecorator(new TracedTaskDecorator(tracer)),
                    new FailsafePlugin().withPolicy(
                            new RetryRequestPolicy(
                                    retryPolicy(transientSocketFaults()))
                                    .withPredicate(new IdempotencyPredicate())),
                    new FailsafePlugin().withPolicy(
                            new RetryRequestPolicy(
                                    retryPolicy(transientConnectionFaults()))
                                    .withPredicate(alwaysTrue())),
                    new AuthorizationPlugin(new PlatformCredentialsAuthorizationProvider("example")),
                    new FailsafePlugin()
                            .withPolicy(new BackupRequest<>(10, MILLISECONDS)),
                    new FailsafePlugin()
                            .withPolicy(Timeout.of(Duration.ofSeconds(3))),
                    new OriginalStackTracePlugin(),
                    new CustomPlugin());
        }

        private RetryPolicy<ClientHttpResponse> retryPolicy(
                final Predicate<Throwable> predicate) {

            return new RetryPolicy<ClientHttpResponse>()
                    .handleIf(predicate)
                    .withBackoff(50, 2000, MILLIS)
                    .withDelay(new CompositeDelayFunction<>(Arrays.asList(
                            new RetryAfterDelayFunction(systemUTC()),
                            new RateLimitResetDelayFunction(systemUTC())
                    )))
                    .withMaxRetries(10)
                    .withMaxDuration(Duration.ofSeconds(2))
                    .withJitter(0.2);
        }

        @Bean
        public RestOperations exampleRestOperations(final Http http) {
            return new HttpOperations(http);
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
                    ThreadPoolExecutors.builder()
                        .elasticSize(1, 20)
                        .keepAlive(1, MINUTES)
                        .withoutQueue()
                        .threadFactory(new CustomizableThreadFactory("http-example-"))
                        .build(),
                    tracer);
        }

        @Bean
        public MeterBinder executorServiceMetrics(final ExecutorService executor) {
            return new ExecutorServiceMetrics(executor, "http-example", singleton(Tag.of("clientId", "example")));
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

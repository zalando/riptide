package org.zalando.riptide.autoconfigure;

import com.fasterxml.jackson.databind.*;
import com.google.common.collect.*;
import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.binder.*;
import io.micrometer.core.instrument.binder.jvm.*;
import io.opentracing.*;
import io.opentracing.contrib.concurrent.*;
import net.jodah.failsafe.*;
import org.apache.http.client.config.*;
import org.apache.http.conn.ssl.*;
import org.apache.http.impl.client.*;
import org.apache.http.ssl.SSLContexts;
import org.springframework.boot.actuate.autoconfigure.metrics.*;
import org.springframework.boot.autoconfigure.jackson.*;
import org.springframework.context.annotation.*;
import org.springframework.http.*;
import org.springframework.http.client.*;
import org.springframework.http.converter.*;
import org.springframework.http.converter.json.*;
import org.springframework.scheduling.concurrent.*;
import org.springframework.web.client.*;
import org.zalando.logbook.*;
import org.zalando.logbook.autoconfigure.*;
import org.zalando.riptide.*;
import org.zalando.riptide.auth.*;
import org.zalando.riptide.autoconfigure.PluginTest.*;
import org.zalando.riptide.backup.*;
import org.zalando.riptide.chaos.*;
import org.zalando.riptide.compatibility.*;
import org.zalando.riptide.failsafe.*;
import org.zalando.riptide.failsafe.metrics.*;
import org.zalando.riptide.faults.*;
import org.zalando.riptide.httpclient.*;
import org.zalando.riptide.idempotency.*;
import org.zalando.riptide.logbook.*;
import org.zalando.riptide.micrometer.*;
import org.zalando.riptide.opentracing.*;
import org.zalando.riptide.soap.*;
import org.zalando.riptide.stream.Streams;
import org.zalando.riptide.timeout.*;
import org.zalando.tracer.*;
import org.zalando.tracer.autoconfigure.*;
import org.zalando.tracer.httpclient.*;

import java.net.*;
import java.time.Clock;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.ThreadPoolExecutor.*;

import static java.time.Clock.*;
import static java.time.temporal.ChronoUnit.*;
import static java.util.Collections.*;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.concurrent.TimeUnit.*;
import static javax.net.ssl.HttpsURLConnection.*;
import static org.zalando.riptide.chaos.FailureInjection.*;

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

package org.zalando.riptide.autoconfigure.retry;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.client.MockRestServiceServer;
import org.zalando.logbook.autoconfigure.LogbookAutoConfiguration;
import org.zalando.riptide.Http;
import org.zalando.riptide.autoconfigure.MetricsTestAutoConfiguration;
import org.zalando.riptide.autoconfigure.OpenTracingTestAutoConfiguration;
import org.zalando.riptide.autoconfigure.RiptideClientTest;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.http.HttpStatus.Series.SERVER_ERROR;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.zalando.riptide.Bindings.anySeries;
import static org.zalando.riptide.Bindings.on;
import static org.zalando.riptide.Navigators.series;
import static org.zalando.riptide.PassRoute.pass;
import static org.zalando.riptide.failsafe.RetryRoute.retry;

@RiptideClientTest
@ActiveProfiles("default")
@Slf4j
public class FailsafeCustomExecutorTest {

    public static class CountingExecutorService extends ThreadPoolExecutor {
        private final String name;
        AtomicInteger counter = new AtomicInteger();

        public CountingExecutorService(String name,
                                       int corePoolSize,
                                       int maximumPoolSize,
                                       long keepAliveTime,
                                       TimeUnit unit,
                                       BlockingQueue<Runnable> workQueue) {
            super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
                    Executors.defaultThreadFactory());
            this.name = name;
        }

        void resetCounter() {
            counter.set(0);
        }

        @Override
        public void execute(@NotNull Runnable command) {
            super.execute(() -> {
                log.info("Failsafe executor runnable " + name);
                counter.incrementAndGet();
                command.run();
            });
        }
    }

    @Configuration
    @ImportAutoConfiguration({
            JacksonAutoConfiguration.class,
            LogbookAutoConfiguration.class,
            OpenTracingTestAutoConfiguration.class,
            MetricsTestAutoConfiguration.class,
    })
    static class ContextConfiguration {
        @Bean(name = "custom-executor-testRetryPolicyExecutorService")
        public ExecutorService retryPolicyExecutorService() {
            return new CountingExecutorService("retryPolicy", 3, 3,
                    0L, TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<>());
        }

        @Bean(name = "custom-executor-testCircuitBreakerExecutorService")
        public ExecutorService circuitBreakerExecutorService() {
            return new CountingExecutorService("circuitBreaker", 3, 3,
                    0L, TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<>());
        }

        @Bean(name = "custom-executor-testBackupRequestExecutorService")
        public ExecutorService backupRequestExecutorService() {
            return new CountingExecutorService("backupRequest", 3, 3,
                    0L, TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<>());
        }

        @Bean(name = "custom-executor-testTimeoutExecutorService")
        public ExecutorService timeoutExecutorService() {
            return new CountingExecutorService("timeout", 3, 3,
                    0L, TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<>());
        }
    }

    @Autowired
    @Qualifier("custom-executor-test")
    private Http httpClient;

    @Autowired
    @Qualifier("custom-executor-testRetryPolicyExecutorService")
    private CountingExecutorService retryPolicyExecutorService;

    @Autowired
    @Qualifier("custom-executor-testCircuitBreakerExecutorService")
    private CountingExecutorService circuitBreakerExecutorService;

    @Autowired
    @Qualifier("custom-executor-testBackupRequestExecutorService")
    private CountingExecutorService backupRequestExecutorService;

    @Autowired
    @Qualifier("custom-executor-testTimeoutExecutorService")
    private CountingExecutorService timeoutExecutorService;

    @Autowired
    private MockRestServiceServer server;

    @Test
    void shouldRetryForAtMostMaxRetriesTimes() {

        int invocationCount = 3;
        server.expect(times(invocationCount), requestTo("http://retry-test")).andRespond(withSuccess());

        IntStream.range(0, invocationCount).forEach(i -> httpClient.get().dispatch(series(),
                        on(SERVER_ERROR).call(retry()), anySeries().call(pass()))
                .join());

        server.verify();
        assertEquals(invocationCount, retryPolicyExecutorService.counter.get());
        assertEquals(invocationCount, circuitBreakerExecutorService.counter.get());
        assertEquals(invocationCount, backupRequestExecutorService.counter.get());
        assertEquals(invocationCount, timeoutExecutorService.counter.get());
    }
}

package org.zalando.riptide.failsafe;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.failsafe.Timeout;
import dev.failsafe.TimeoutExceededException;
import lombok.extern.slf4j.Slf4j;
import nl.altindag.log.LogCaptor;
import okhttp3.mockwebserver.MockWebServer;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.logging.LoggerFactory;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.slf4j.event.LoggingEvent;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.zalando.riptide.Http;
import org.zalando.riptide.httpclient.ApacheClientHttpRequestFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.zalando.riptide.PassRoute.pass;
import static org.zalando.riptide.failsafe.MockWebServerUtil.emptyMockResponse;
import static org.zalando.riptide.failsafe.MockWebServerUtil.getBaseUrl;
import static org.zalando.riptide.failsafe.MockWebServerUtil.verify;

@Slf4j
final class FailsafePluginCustomExecutorTest {

    private static class CountingExecutorService extends ThreadPoolExecutor {
        AtomicInteger counter = new AtomicInteger();

        public CountingExecutorService(int corePoolSize,
                                       int maximumPoolSize,
                                       long keepAliveTime,
                                       TimeUnit unit,
                                       BlockingQueue<Runnable> workQueue) {
            super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
                    Executors.defaultThreadFactory());
        }

        void resetCounter() {
            counter.set(0);
        }

        @Override
        public void execute(@Nonnull Runnable command) {
            super.execute(() -> {
                log.info("Failsafe executor runnable");
                counter.incrementAndGet();
                command.run();
            });
        }
    }

    private final MockWebServer server = new MockWebServer();

    private final CloseableHttpClient client = HttpClientBuilder.create().build();
    private final ApacheClientHttpRequestFactory factory = new ApacheClientHttpRequestFactory(client);

    private final CountingExecutorService countingExecutor = new CountingExecutorService(3, 3,
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>());

    private final Http unit = Http.builder()
            .executor(newFixedThreadPool(2))
            .requestFactory(factory)
            .baseUrl(getBaseUrl(server))
            .converter(createJsonConverter())
            .plugin(new FailsafePlugin()
                    .withPolicy(Timeout.of(Duration.ofSeconds(1)))
                    .withExecutor(countingExecutor)
            )
            .build();

    private static MappingJackson2HttpMessageConverter createJsonConverter() {
        final MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setObjectMapper(createObjectMapper());
        return converter;
    }

    private static ObjectMapper createObjectMapper() {
        return new ObjectMapper().findAndRegisterModules()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    @AfterEach
    void tearDown() throws IOException {
        client.close();
        server.shutdown();
        countingExecutor.resetCounter();
    }

    @Test
    void shouldUseCustomExecutor() throws InterruptedException {
        server.enqueue(emptyMockResponse());

        int invocationCount = 5;
        var futures = IntStream.range(0, invocationCount).mapToObj(i -> {
            server.enqueue(emptyMockResponse());

            return unit.get("/foo")
                    .call(pass());
        }).toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .join();

        verify(server, invocationCount, "/foo");
        assertEquals(invocationCount, countingExecutor.counter.get());
    }

    @Test
    void shouldTimeout() {
        server.enqueue(emptyMockResponse().setHeadersDelay(2, SECONDS));

        final CompletionException exception = assertThrows(CompletionException.class,
                unit.get("/foo")
                        .call(pass())::join);

        assertTrue(exception.getCause() instanceof TimeoutExceededException);

        verify(server, 1, "/foo");
        assertEquals(1, countingExecutor.counter.get());
    }

    @Test
    void shouldLogWarningOnSingleThreadedExecutor() {
        LogCaptor logCaptor = LogCaptor.forClass(FailsafePlugin.class);
        new FailsafePlugin().withExecutor(Executors.newFixedThreadPool(1));
        assertEquals(1, logCaptor.getWarnLogs().size());
    }

    @Test
    void shouldNotLogWarningOnMultiThreadedExecutor() {
        LogCaptor logCaptor = LogCaptor.forClass(FailsafePlugin.class);
        new FailsafePlugin().withExecutor(ForkJoinPool.commonPool());
        assertEquals(0, logCaptor.getWarnLogs().size());
    }

}

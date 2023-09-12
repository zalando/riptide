package org.zalando.riptide.failsafe;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.failsafe.Timeout;
import dev.failsafe.TimeoutExceededException;
import lombok.extern.slf4j.Slf4j;
import okhttp3.mockwebserver.MockWebServer;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.zalando.riptide.Http;
import org.zalando.riptide.httpclient.ApacheClientHttpRequestFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.zalando.riptide.PassRoute.pass;
import static org.zalando.riptide.failsafe.MockWebServerUtil.emptyMockResponse;
import static org.zalando.riptide.failsafe.MockWebServerUtil.getBaseUrl;
import static org.zalando.riptide.failsafe.MockWebServerUtil.verify;

@Slf4j
final class FailsafePluginCustomExecutorTest {

    public static class CountingExecutorService extends ThreadPoolExecutor {
        AtomicInteger counter = new AtomicInteger();
        public CountingExecutorService (int corePoolSize,
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
        public void execute(@NotNull Runnable command) {
            counter.incrementAndGet();
            super.execute(command);
        }
    }

    private final MockWebServer server = new MockWebServer();

    private final CloseableHttpClient client = HttpClientBuilder.create().build();
    private final ApacheClientHttpRequestFactory factory = new ApacheClientHttpRequestFactory(client);

    private final CountingExecutorService countingExecutor = new CountingExecutorService(1, 1,
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>());

    private final Http unit = Http.builder()
            .executor(newSingleThreadExecutor())
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
    void shouldUseCustomExecutor() {
        server.enqueue(emptyMockResponse());

        int invocationCount = 5;
        IntStream.range(0, invocationCount).forEach(i -> {
            server.enqueue(emptyMockResponse());

            unit.get("/foo")
                .call(pass())
                .join();
        });
        verify(server, invocationCount, "/foo");
        assertEquals(invocationCount, countingExecutor.counter.get());
    }

    @Test
    void shouldTimeout() {
        server.enqueue(emptyMockResponse().setHeadersDelay(2, SECONDS));

        final CompletionException exception = assertThrows(CompletionException.class,
                unit.get("/foo")
                        .call(pass())::join);

        assertThat(exception.getCause(), is(instanceOf(TimeoutExceededException.class)));

        verify(server, 1, "/foo");
        assertEquals(1, countingExecutor.counter.get());
    }

}

package org.zalando.riptide;

import com.github.restdriver.clientdriver.ClientDriver;
import com.github.restdriver.clientdriver.ClientDriverFactory;
import io.netty.channel.nio.NioEventLoopGroup;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.HttpComponentsAsyncClientHttpRequestFactory;
import org.springframework.http.client.Netty4ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.zalando.riptide.Http.ConfigurationStage;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicReference;

import static com.github.restdriver.clientdriver.RestClientDriver.giveEmptyResponse;
import static com.github.restdriver.clientdriver.RestClientDriver.onRequestTo;
import static java.lang.Thread.currentThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.HttpStatus.Series.SUCCESSFUL;
import static org.zalando.riptide.Bindings.on;
import static org.zalando.riptide.Navigators.series;

final class ThreadAffinityTest {

    @Test
    void syncBlocking() {
        final ConfigurationStage stage = Http.builder()
                .executor(Runnable::run)
                .requestFactory(new SimpleClientHttpRequestFactory());

        test(stage, "main", "main", "main");
    }

    @Test
    void asyncBlocking() {
        final ConfigurationStage stage = Http.builder()
                .executor(Executors.newSingleThreadExecutor(threadFactory("process")))
                .requestFactory(new SimpleClientHttpRequestFactory());

        test(stage, "process", "process", "process");
    }

    @Test
    void syncNonBlockingApache() throws Exception {
        final HttpComponentsAsyncClientHttpRequestFactory requestFactory =
                new HttpComponentsAsyncClientHttpRequestFactory(HttpAsyncClientBuilder.create()
                        .setThreadFactory(threadFactory("io"))
                        .build());

        try {
            final ConfigurationStage stage = Http.builder()
                    .asyncRequestFactory(requestFactory);

            test(stage, "main", "io", "io");
        } finally {
            requestFactory.destroy();
        }
    }

    @Test
    void syncNonBlockingNetty() throws Exception {
        final Netty4ClientHttpRequestFactory requestFactory = new Netty4ClientHttpRequestFactory(
                new NioEventLoopGroup(0, threadFactory("io")));

        try {
            final ConfigurationStage stage = Http.builder()
                    .asyncRequestFactory(requestFactory);

            test(stage, "main", "io", "io");
        } finally {
            requestFactory.destroy();
        }
    }

    void test(final ConfigurationStage stage, final String request, final String dispatch, final String callback) {
        final ClientDriver driver = new ClientDriverFactory().createClientDriver();

        try {
            driver.addExpectation(onRequestTo("/"), giveEmptyResponse());

            final Http http = stage
                    .baseUrl(driver.getBaseUrl())
                    .build();

            final AtomicReference<Thread> requestThread = new AtomicReference<>();
            final AtomicReference<Thread> dispatchThread = new AtomicReference<>();
            final AtomicReference<Thread> callbackThread = new AtomicReference<>();

            http.get("/")
                    .body(message ->
                            requestThread.set(currentThread()))
                    .dispatch(series(),
                            on(SUCCESSFUL).call((response, reader) ->
                                    dispatchThread.set(currentThread())))
                    .whenComplete((response, exception) ->
                            callbackThread.set(currentThread()))
            .join();

            assertEquals(request, requestThread.get().getName(), "request thread");
            assertEquals(dispatch, callbackThread.get().getName(), "callback thread");
            assertEquals(callback, dispatchThread.get().getName(), "dispatch thread");
        } finally {
            driver.verify();
        }
    }

    private ThreadFactory threadFactory(final String name) {
        return runnable -> {
            final Thread thread = new Thread(runnable);
            thread.setName(name);
            thread.setDaemon(true);
            return thread;
        };
    }

}

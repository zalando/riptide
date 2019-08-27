package org.zalando.riptide;

import com.github.restdriver.clientdriver.*;
import io.netty.channel.nio.*;
import org.apache.http.impl.nio.client.*;
import org.junit.jupiter.api.*;
import org.springframework.http.client.*;
import org.zalando.riptide.Http.*;

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import static com.github.restdriver.clientdriver.RestClientDriver.*;
import static java.lang.Thread.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.HttpStatus.Series.*;
import static org.zalando.riptide.Bindings.*;
import static org.zalando.riptide.Navigators.*;

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
